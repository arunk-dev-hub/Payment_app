package com.payment.paymentservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Throttle payment + refund creation; reads are left unthrottled.
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/api/v1/payments", "/api/v1/payments/*/refunds");
    }
}
