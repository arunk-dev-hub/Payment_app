package com.payment.paymentservice.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class StripeConfig {

    @Value("${stripe.api.key:}")
    private String apiKey;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Stripe API key is not configured. Stripe integration operations will fail.");
        } else {
            Stripe.apiKey = apiKey;
            log.info("Stripe SDK initialized successfully.");
        }
    }
}
