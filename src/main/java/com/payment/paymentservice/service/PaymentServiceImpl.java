package com.payment.paymentservice.service;

import com.payment.paymentservice.dto.PaymentRequest;
import com.payment.paymentservice.dto.PaymentResponse;
import com.payment.paymentservice.exception.IdempotencyConflictException;
import com.payment.paymentservice.exception.InvalidPaymentStatusTransitionException;
import com.payment.paymentservice.exception.PaymentNotFoundException;
import com.payment.paymentservice.model.Payment;
import com.payment.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

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

        if (currentStatus != Payment.PaymentStatus.PENDING) {
            throw new InvalidPaymentStatusTransitionException(
                    "Payment status cannot be changed from " + currentStatus + " to " + requestedStatus
            );
        }

        if (requestedStatus == Payment.PaymentStatus.PENDING) {
            return;
        }

        if (requestedStatus != Payment.PaymentStatus.COMPLETED && requestedStatus != Payment.PaymentStatus.FAILED) {
            throw new InvalidPaymentStatusTransitionException(
                    "Payment status can only move from PENDING to COMPLETED or FAILED"
            );
        }
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
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
