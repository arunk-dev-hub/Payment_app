package com.payment.paymentservice.repository;

import com.payment.paymentservice.model.Payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
