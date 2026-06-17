package com.payment.paymentservice.service;

import com.payment.paymentservice.dto.PaymentRequest;
import com.payment.paymentservice.dto.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    PaymentResponse createPayment(PaymentRequest request, String idempotencyKey);

    PaymentResponse getPaymentById(String id);

    Page<PaymentResponse> getAllPayments(String status, String currency, String paymentMethod, Pageable pageable);

    PaymentResponse updatePaymentStatus(String id, String status);

    void processStripeWebhook(String payload, String sigHeader);
}
