package com.payment.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private String id;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String idempotencyKey;
    private String status;
    private String stripePaymentIntentId;
    private String stripeClientSecret;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
