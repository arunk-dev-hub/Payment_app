package com.payment.paymentservice.service;

import com.payment.paymentservice.dto.PaymentRequest;
import com.payment.paymentservice.dto.PaymentResponse;
import com.payment.paymentservice.exception.IdempotencyConflictException;
import com.payment.paymentservice.exception.InvalidPaymentStatusTransitionException;
import com.payment.paymentservice.exception.PaymentNotFoundException;
import com.payment.paymentservice.model.Payment;
import com.payment.paymentservice.repository.PaymentRepository;
import com.payment.paymentservice.repository.ProcessedStripeEventRepository;
import com.payment.paymentservice.model.ProcessedStripeEvent;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.net.Webhook;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Map;
import java.util.HashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTests {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StripeService stripeService;

    @Mock
    private ProcessedStripeEventRepository processedStripeEventRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository, stripeService, processedStripeEventRepository);
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "whsec_test_secret");
    }

    @Test
    void createPayment_shouldReturnPaymentResponse() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(BigDecimal.valueOf(150.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .build();

        Payment mockSavedPayment = Payment.builder()
                .id("test-id-123")
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .idempotencyKey("create-payment-key")
                .status(Payment.PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        com.stripe.model.PaymentIntent mockPaymentIntent = mock(com.stripe.model.PaymentIntent.class);
        when(mockPaymentIntent.getId()).thenReturn("pi_mock_123");
        when(mockPaymentIntent.getClientSecret()).thenReturn("pi_mock_123_secret_xyz");

        when(paymentRepository.findByIdempotencyKey("create-payment-key")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockSavedPayment);
        when(stripeService.createPaymentIntent(any(), any(), any())).thenReturn(mockPaymentIntent);

        PaymentResponse response = paymentService.createPayment(request, "create-payment-key");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("test-id-123");
        assertThat(response.getAmount()).isEqualTo(BigDecimal.valueOf(150.00));
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getIdempotencyKey()).isEqualTo("create-payment-key");

        verify(paymentRepository, times(1)).findByIdempotencyKey("create-payment-key");
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void createPayment_shouldReturnExistingPayment_whenIdempotencyKeyMatchesSameRequest() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(BigDecimal.valueOf(150.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .build();

        Payment existingPayment = Payment.builder()
                .id("test-id-123")
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .idempotencyKey("same-key")
                .status(Payment.PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existingPayment));

        PaymentResponse response = paymentService.createPayment(request, "same-key");

        assertThat(response.getId()).isEqualTo("test-id-123");
        assertThat(response.getIdempotencyKey()).isEqualTo("same-key");

        verify(paymentRepository, times(1)).findByIdempotencyKey("same-key");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPayment_shouldThrowException_whenIdempotencyKeyMatchesDifferentRequest() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(BigDecimal.valueOf(200.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .build();

        Payment existingPayment = Payment.builder()
                .id("test-id-123")
                .amount(BigDecimal.valueOf(150.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .idempotencyKey("same-key")
                .status(Payment.PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentService.createPayment(request, "same-key"))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("Idempotency key was already used with different payment details");

        verify(paymentRepository, times(1)).findByIdempotencyKey("same-key");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void getPaymentById_shouldReturnPaymentResponse_whenPaymentExists() {
        Payment mockPayment = Payment.builder()
                .id("test-id-123")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .idempotencyKey("test-key")
                .status(Payment.PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById("test-id-123")).thenReturn(Optional.of(mockPayment));

        PaymentResponse response = paymentService.getPaymentById("test-id-123");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("test-id-123");
        assertThat(response.getAmount()).isEqualTo(BigDecimal.valueOf(100.00));

        verify(paymentRepository, times(1)).findById("test-id-123");
    }

    @Test
    void getPaymentById_shouldThrowException_whenPaymentDoesNotExist() {
        when(paymentRepository.findById("non-existent-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById("non-existent-id"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found with id: non-existent-id");

        verify(paymentRepository, times(1)).findById("non-existent-id");
    }

    @Test
    void getAllPayments_shouldReturnPage() {
        Payment mockPayment = Payment.builder()
                .id("test-id-123")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .status(Payment.PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockPayment)));

        Page<PaymentResponse> responses = paymentService.getAllPayments(
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().get(0).getId()).isEqualTo("test-id-123");

        verify(paymentRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void updatePaymentStatus_shouldMovePendingPaymentToCompleted() {
        Payment pendingPayment = Payment.builder()
                .id("test-id-123")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .idempotencyKey("test-key")
                .status(Payment.PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Payment completedPayment = Payment.builder()
                .id("test-id-123")
                .amount(pendingPayment.getAmount())
                .currency(pendingPayment.getCurrency())
                .paymentMethod(pendingPayment.getPaymentMethod())
                .idempotencyKey(pendingPayment.getIdempotencyKey())
                .status(Payment.PaymentStatus.COMPLETED)
                .createdAt(pendingPayment.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById("test-id-123")).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(completedPayment);

        PaymentResponse response = paymentService.updatePaymentStatus("test-id-123", "COMPLETED");

        assertThat(response.getStatus()).isEqualTo("COMPLETED");

        verify(paymentRepository, times(1)).findById("test-id-123");
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void updatePaymentStatus_shouldThrowException_whenFinalPaymentIsChanged() {
        Payment completedPayment = Payment.builder()
                .id("test-id-123")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .status(Payment.PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById("test-id-123")).thenReturn(Optional.of(completedPayment));

        assertThatThrownBy(() -> paymentService.updatePaymentStatus("test-id-123", "FAILED"))
                .isInstanceOf(InvalidPaymentStatusTransitionException.class)
                .hasMessageContaining("Payment status cannot be changed from COMPLETED to FAILED");

        verify(paymentRepository, times(1)).findById("test-id-123");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processStripeWebhook_shouldTransitionToCompleted_whenPaymentIntentSucceeded() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_111");
        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        com.stripe.model.PaymentIntent mockPaymentIntent = mock(com.stripe.model.PaymentIntent.class);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("paymentId", "payment-uuid");
        when(mockPaymentIntent.getMetadata()).thenReturn(metadata);
        when(deserializer.getObject()).thenReturn(Optional.of(mockPaymentIntent));

        Payment pendingPayment = Payment.builder()
                .id("payment-uuid")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .status(Payment.PaymentStatus.PENDING)
                .stripePaymentIntentId("pi_123")
                .build();

        when(processedStripeEventRepository.existsById("evt_111")).thenReturn(false);
        when(paymentRepository.findById("payment-uuid")).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(mockEvent);

            paymentService.processStripeWebhook("payload", "sigHeader");
        }

        assertThat(pendingPayment.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        verify(processedStripeEventRepository, times(1)).saveAndFlush(any(ProcessedStripeEvent.class));
        verify(paymentRepository, times(1)).save(pendingPayment);
    }

    @Test
    void processStripeWebhook_shouldTransitionToFailed_whenPaymentIntentFailed() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_222");
        when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        com.stripe.model.PaymentIntent mockPaymentIntent = mock(com.stripe.model.PaymentIntent.class);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("paymentId", "payment-uuid");
        when(mockPaymentIntent.getMetadata()).thenReturn(metadata);
        when(deserializer.getObject()).thenReturn(Optional.of(mockPaymentIntent));

        Payment pendingPayment = Payment.builder()
                .id("payment-uuid")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .status(Payment.PaymentStatus.PENDING)
                .stripePaymentIntentId("pi_123")
                .build();

        when(processedStripeEventRepository.existsById("evt_222")).thenReturn(false);
        when(paymentRepository.findById("payment-uuid")).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(mockEvent);

            paymentService.processStripeWebhook("payload", "sigHeader");
        }

        assertThat(pendingPayment.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        verify(processedStripeEventRepository, times(1)).saveAndFlush(any(ProcessedStripeEvent.class));
        verify(paymentRepository, times(1)).save(pendingPayment);
    }

    @Test
    void processStripeWebhook_shouldDeduplicate_whenEventAlreadyProcessed() {
        when(processedStripeEventRepository.existsById("evt_already_processed")).thenReturn(true);

        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_already_processed");

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(mockEvent);

            paymentService.processStripeWebhook("payload", "sigHeader");
        }

        verify(processedStripeEventRepository, never()).save(any(ProcessedStripeEvent.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
