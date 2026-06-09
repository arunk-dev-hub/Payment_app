package com.payment.paymentservice.repository;

import com.payment.paymentservice.model.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, String> {

    List<Refund> findByPaymentIdOrderByCreatedAtDesc(String paymentId);

    @Query("select coalesce(sum(refund.amount), 0) from Refund refund where refund.payment.id = :paymentId and refund.status = 'COMPLETED'")
    BigDecimal getCompletedRefundAmountByPaymentId(@Param("paymentId") String paymentId);
}
