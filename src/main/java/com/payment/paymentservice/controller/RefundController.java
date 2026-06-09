package com.payment.paymentservice.controller;

import com.payment.paymentservice.dto.RefundRequest;
import com.payment.paymentservice.dto.RefundResponse;
import com.payment.paymentservice.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments/{paymentId}/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<RefundResponse> createRefund(
            @PathVariable String paymentId,
            @Valid @RequestBody RefundRequest request
    ) {
        RefundResponse response = refundService.createRefund(paymentId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RefundResponse>> getRefundsByPaymentId(@PathVariable String paymentId) {
        List<RefundResponse> responses = refundService.getRefundsByPaymentId(paymentId);
        return ResponseEntity.ok(responses);
    }
}
