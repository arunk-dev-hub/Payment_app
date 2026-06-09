package com.payment.paymentservice.service;

import com.payment.paymentservice.dto.RefundRequest;
import com.payment.paymentservice.dto.RefundResponse;
import com.payment.paymentservice.exception.InvalidRefundException;
import com.payment.paymentservice.exception.PaymentNotFoundException;
import com.payment.paymentservice.model.Payment;
import com.payment.paymentservice.model.Refund;
import com.payment.paymentservice.repository.PaymentRepository;
import com.payment.paymentservice.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    @Override
    @Transactional
    public RefundResponse createRefund(String paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));

        validateRefund(payment, request.getAmount());

        Refund refund = Refund.builder()
                .payment(payment)
                .amount(request.getAmount())
                .reason(request.getReason())
                .status(Refund.RefundStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        Refund savedRefund = refundRepository.save(refund);
        return mapToResponse(savedRefund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByPaymentId(String paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new PaymentNotFoundException("Payment not found with id: " + paymentId);
        }

        return refundRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateRefund(Payment payment, BigDecimal requestedRefundAmount) {
        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new InvalidRefundException("Only completed payments can be refunded");
        }

        BigDecimal alreadyRefundedAmount = refundRepository.getCompletedRefundAmountByPaymentId(payment.getId());
        BigDecimal availableRefundAmount = payment.getAmount().subtract(alreadyRefundedAmount);

        if (requestedRefundAmount.compareTo(availableRefundAmount) > 0) {
            throw new InvalidRefundException(
                    "Refund amount exceeds available refundable amount: " + availableRefundAmount
            );
        }
    }

    private RefundResponse mapToResponse(Refund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .paymentId(refund.getPayment().getId())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .status(refund.getStatus().name())
                .createdAt(refund.getCreatedAt())
                .build();
    }
}
