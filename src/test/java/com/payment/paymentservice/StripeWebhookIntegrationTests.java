package com.payment.paymentservice;

import com.payment.paymentservice.model.Payment;
import com.payment.paymentservice.repository.PaymentRepository;
import com.payment.paymentservice.repository.ProcessedStripeEventRepository;
import com.payment.paymentservice.service.StripeService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StripeWebhookIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProcessedStripeEventRepository processedStripeEventRepository;

    @MockitoBean
    private StripeService stripeService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        processedStripeEventRepository.deleteAll();
    }

    @Test
    void webhook_shouldTransitionPaymentToCompleted_endToEnd() throws Exception {
        // 1. Create a payment locally
        Payment payment = Payment.builder()
                .amount(BigDecimal.valueOf(250.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .status(Payment.PaymentStatus.PENDING)
                .stripePaymentIntentId("pi_integration_123")
                .stripeClientSecret("pi_secret_123")
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        // 2. Prepare mock Stripe event
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_integration_999");
        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        com.stripe.model.PaymentIntent mockPaymentIntent = mock(com.stripe.model.PaymentIntent.class);
        when(mockPaymentIntent.getId()).thenReturn("pi_integration_123");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("paymentId", savedPayment.getId());
        when(mockPaymentIntent.getMetadata()).thenReturn(metadata);
        when(deserializer.getObject()).thenReturn(Optional.of(mockPaymentIntent));

        // 3. Perform webhook request while mocking Webhook.constructEvent
        String payload = "{\"id\": \"evt_integration_999\"}";
        String sigHeader = "t=123,v1=abc";

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(eq(payload), eq(sigHeader), any()))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", sigHeader)
                            .content(payload))
                    .andExpect(status().isOk());
        }

        // 4. Verify Payment state transition in the actual database
        Payment updatedPayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        
        // 5. Verify event deduplication table populated
        assertThat(processedStripeEventRepository.existsById("evt_integration_999")).isTrue();
    }
}
