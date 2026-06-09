package com.payment.paymentservice.service;

import com.payment.paymentservice.dto.RefundRequest;
import com.payment.paymentservice.dto.RefundResponse;

import java.util.List;

public interface RefundService {

    RefundResponse createRefund(String paymentId, RefundRequest request);

    List<RefundResponse> getRefundsByPaymentId(String paymentId);
}
