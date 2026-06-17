package com.payment.paymentservice.service;

import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;

import java.math.BigDecimal;

public interface StripeService {

    PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String paymentId);

    Refund createRefund(String paymentIntentId, BigDecimal amount);
}
