package com.payment.paymentservice.controller;

import com.payment.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stripe Webhook", description = "Endpoints for Stripe webhook event subscription")
public class StripeWebhookController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Handle Stripe Webhook Events", description = "Endpoint for Stripe to asynchronously notify payment state updates")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        log.info("Received Stripe Webhook call");
        try {
            paymentService.processStripeWebhook(payload, sigHeader);
            return ResponseEntity.ok("Success");
        } catch (IllegalArgumentException e) {
            log.error("Invalid webhook event signature or payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Signature verification failed: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Webhook processing state error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Configuration error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unhandled error processing Stripe Webhook", e);
            return ResponseEntity.internalServerError().body("Internal server error: " + e.getMessage());
        }
    }
}
