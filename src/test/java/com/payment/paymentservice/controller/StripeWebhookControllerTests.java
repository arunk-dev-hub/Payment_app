package com.payment.paymentservice.controller;

import com.payment.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(StripeWebhookController.class)
class StripeWebhookControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void handleStripeWebhook_shouldReturnOk_whenProcessingSucceeds() throws Exception {
        String payload = "{\"id\": \"evt_123\"}";
        String sigHeader = "t=123,v1=abc";

        doNothing().when(paymentService).processStripeWebhook(payload, sigHeader);

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Success"));

        verify(paymentService, times(1)).processStripeWebhook(payload, sigHeader);
    }

    @Test
    void handleStripeWebhook_shouldReturnBadRequest_whenSignatureInvalid() throws Exception {
        String payload = "{\"id\": \"evt_123\"}";
        String sigHeader = "t=123,v1=abc";

        doThrow(new IllegalArgumentException("Invalid signature")).when(paymentService)
                .processStripeWebhook(payload, sigHeader);

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Signature verification failed: Invalid signature"));

        verify(paymentService, times(1)).processStripeWebhook(payload, sigHeader);
    }

    @Test
    void handleStripeWebhook_shouldReturnInternalServerError_whenConfigError() throws Exception {
        String payload = "{\"id\": \"evt_123\"}";
        String sigHeader = "t=123,v1=abc";

        doThrow(new IllegalStateException("Signing secret is not configured")).when(paymentService)
                .processStripeWebhook(payload, sigHeader);

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(payload))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Configuration error: Signing secret is not configured"));

        verify(paymentService, times(1)).processStripeWebhook(payload, sigHeader);
    }
}
