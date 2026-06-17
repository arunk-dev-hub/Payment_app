package com.payment.paymentservice.service;

import com.payment.paymentservice.exception.StripeIntegrationException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class StripeServiceImpl implements StripeService {

    @Override
    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String paymentId) {
        log.info("Creating Stripe PaymentIntent for paymentId: {}, amount: {}, currency: {}", paymentId, amount, currency);

        long stripeAmount = calculateStripeAmount(amount, currency);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(stripeAmount)
                .setCurrency(currency.toLowerCase())
                .putMetadata("paymentId", paymentId)
                .build();

        try {
            return PaymentIntent.create(params);
        } catch (StripeException exception) {
            log.error("Failed to create Stripe PaymentIntent for paymentId: {}", paymentId, exception);
            throw new StripeIntegrationException("Stripe PaymentIntent creation failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public Refund createRefund(String paymentIntentId, BigDecimal amount) {
        log.info("Creating Stripe Refund for PaymentIntent: {}, amount: {}", paymentIntentId, amount);

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            long stripeAmount = calculateStripeAmount(amount, paymentIntent.getCurrency());

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(stripeAmount)
                    .build();

            return Refund.create(params);
        } catch (StripeException exception) {
            log.error("Failed to create Stripe Refund for PaymentIntent: {}", paymentIntentId, exception);
            throw new StripeIntegrationException("Stripe Refund creation failed: " + exception.getMessage(), exception);
        }
    }

    private long calculateStripeAmount(BigDecimal amount, String currency) {
        String upperCurrency = currency.toUpperCase();
        if (upperCurrency.equals("JPY") || upperCurrency.equals("KRW") || upperCurrency.equals("CLP")) {
            return amount.longValue();
        }
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
}
