package com.payment.paymentservice.service;

import com.payment.paymentservice.dto.PaymentRequest;
import com.payment.paymentservice.dto.PaymentResponse;
import com.payment.paymentservice.exception.IdempotencyConflictException;
import com.payment.paymentservice.exception.InvalidPaymentStatusTransitionException;
import com.payment.paymentservice.exception.PaymentNotFoundException;
import com.payment.paymentservice.model.Payment;
import com.payment.paymentservice.model.ProcessedStripeEvent;
import com.payment.paymentservice.repository.PaymentRepository;
import com.payment.paymentservice.repository.ProcessedStripeEventRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final StripeService stripeService;
    private final ProcessedStripeEventRepository processedStripeEventRepository;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request, String idempotencyKey) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

        if (normalizedIdempotencyKey != null) {
            Payment existingPayment = paymentRepository.findByIdempotencyKey(normalizedIdempotencyKey)
                    .orElse(null);

            if (existingPayment != null) {
                validateIdempotentRequest(existingPayment, request);
                return mapToResponse(existingPayment);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .idempotencyKey(normalizedIdempotencyKey)
                .status(Payment.PaymentStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            Payment savedPayment = paymentRepository.save(payment);

            // Create Stripe PaymentIntent and associate it
            com.stripe.model.PaymentIntent paymentIntent = stripeService.createPaymentIntent(
                    savedPayment.getAmount(),
                    savedPayment.getCurrency(),
                    savedPayment.getId()
            );

            savedPayment.setStripePaymentIntentId(paymentIntent.getId());
            savedPayment.setStripeClientSecret(paymentIntent.getClientSecret());
            savedPayment = paymentRepository.save(savedPayment);

            return mapToResponse(savedPayment);
        } catch (DataIntegrityViolationException exception) {
            if (normalizedIdempotencyKey == null) {
                throw exception;
            }

            Payment existingPayment = paymentRepository.findByIdempotencyKey(normalizedIdempotencyKey)
                    .orElseThrow(() -> exception);

            validateIdempotentRequest(existingPayment, request);
            return mapToResponse(existingPayment);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(String status, String currency, String paymentMethod, Pageable pageable) {
        Specification<Payment> specification = buildPaymentSpecification(status, currency, paymentMethod);
        return paymentRepository.findAll(specification, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(String id, String status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));

        Payment.PaymentStatus requestedStatus = parsePaymentStatus(status);
        validateStatusTransition(payment.getStatus(), requestedStatus);

        payment.setStatus(requestedStatus);
        payment.setUpdatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);
        return mapToResponse(savedPayment);
    }

    @Override
    @Transactional
    public void processStripeWebhook(String payload, String sigHeader) {
        log.info("Received Stripe webhook signature verification");

        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Stripe webhook signing secret is not configured.");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed", e);
            throw new IllegalArgumentException("Invalid signature: " + e.getMessage(), e);
        }

        // Deduplicate events
        if (processedStripeEventRepository.existsById(event.getId())) {
            log.info("Stripe event {} already processed. Skipping.", event.getId());
            return;
        }

        // Save event to deduplicate
        ProcessedStripeEvent processedEvent = ProcessedStripeEvent.builder()
                .eventId(event.getId())
                .processedAt(LocalDateTime.now())
                .build();
        try {
            processedStripeEventRepository.saveAndFlush(processedEvent);
        } catch (DataIntegrityViolationException e) {
            log.info("Stripe event {} already processed (DB conflict). Skipping.", event.getId());
            return;
        }

        if (event.getType().startsWith("payment_intent.")) {
            com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElseGet(() -> {
                        log.warn("Stripe API version mismatch or schema mismatch. Attempting unsafe deserialization.");
                        try {
                            return (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer().deserializeUnsafe();
                        } catch (com.stripe.exception.EventDataObjectDeserializationException e) {
                            throw new IllegalArgumentException("Failed to deserialize Stripe event unsafely: " + e.getMessage(), e);
                        }
                    });



            String paymentId = paymentIntent.getMetadata().get("paymentId");
            Payment payment = null;

            if (paymentId != null) {
                payment = paymentRepository.findById(paymentId).orElse(null);
            }

            if (payment == null) {
                payment = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId()).orElse(null);
            }

            if (payment == null) {
                log.warn("Payment not found for Stripe PaymentIntent: {} / metadata paymentId: {}", 
                        paymentIntent.getId(), paymentId);
                return;
            }

            Payment.PaymentStatus targetStatus;
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    targetStatus = Payment.PaymentStatus.COMPLETED;
                    break;
                case "payment_intent.payment_failed":
                    targetStatus = Payment.PaymentStatus.FAILED;
                    break;
                case "payment_intent.processing":
                    targetStatus = Payment.PaymentStatus.PROCESSING;
                    break;
                default:
                    log.info("Ignoring unhandled payment_intent event type: {}", event.getType());
                    return;
            }

            log.info("Transitioning payment {} from {} to {}", payment.getId(), payment.getStatus(), targetStatus);
            validateStatusTransition(payment.getStatus(), targetStatus);
            payment.setStatus(targetStatus);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        } else {
            log.info("Ignoring unhandled Stripe event type: {}", event.getType());
        }
    }

    private Specification<Payment> buildPaymentSpecification(String status, String currency, String paymentMethod) {
        return Specification
                .where(hasStatus(status))
                .and(hasCurrency(currency))
                .and(hasPaymentMethod(paymentMethod));
    }

    private Specification<Payment> hasStatus(String status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null || status.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            Payment.PaymentStatus paymentStatus = parsePaymentStatus(status);
            return criteriaBuilder.equal(root.get("status"), paymentStatus);
        };
    }

    private Specification<Payment> hasCurrency(String currency) {
        return (root, query, criteriaBuilder) -> {
            if (currency == null || currency.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(criteriaBuilder.upper(root.get("currency")), currency.toUpperCase());
        };
    }

    private Specification<Payment> hasPaymentMethod(String paymentMethod) {
        return (root, query, criteriaBuilder) -> {
            if (paymentMethod == null || paymentMethod.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(criteriaBuilder.upper(root.get("paymentMethod")), paymentMethod.toUpperCase());
        };
    }

    private Payment.PaymentStatus parsePaymentStatus(String status) {
        try {
            return Payment.PaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid payment status: " + status);
        }
    }

    private void validateStatusTransition(Payment.PaymentStatus currentStatus, Payment.PaymentStatus requestedStatus) {
        if (currentStatus == requestedStatus) {
            return;
        }

        if (currentStatus == Payment.PaymentStatus.PENDING) {
            if (requestedStatus == Payment.PaymentStatus.PROCESSING ||
                requestedStatus == Payment.PaymentStatus.COMPLETED ||
                requestedStatus == Payment.PaymentStatus.FAILED) {
                return;
            }
        } else if (currentStatus == Payment.PaymentStatus.PROCESSING) {
            if (requestedStatus == Payment.PaymentStatus.COMPLETED ||
                requestedStatus == Payment.PaymentStatus.FAILED) {
                return;
            }
        } else if (currentStatus == Payment.PaymentStatus.COMPLETED) {
            if (requestedStatus == Payment.PaymentStatus.REFUNDED) {
                return;
            }
        }

        throw new InvalidPaymentStatusTransitionException(
                "Payment status cannot be changed from " + currentStatus + " to " + requestedStatus
        );
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        return idempotencyKey.trim();
    }

    private void validateIdempotentRequest(Payment existingPayment, PaymentRequest request) {
        boolean sameRequest = sameAmount(existingPayment.getAmount(), request.getAmount())
                && Objects.equals(existingPayment.getCurrency(), request.getCurrency())
                && Objects.equals(existingPayment.getPaymentMethod(), request.getPaymentMethod());

        if (!sameRequest) {
            throw new IdempotencyConflictException(
                    "Idempotency key was already used with different payment details"
            );
        }
    }

    private boolean sameAmount(BigDecimal firstAmount, BigDecimal secondAmount) {
        if (firstAmount == null || secondAmount == null) {
            return Objects.equals(firstAmount, secondAmount);
        }

        return firstAmount.compareTo(secondAmount) == 0;
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .idempotencyKey(payment.getIdempotencyKey())
                .status(payment.getStatus().name())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .stripeClientSecret(payment.getStripeClientSecret())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
