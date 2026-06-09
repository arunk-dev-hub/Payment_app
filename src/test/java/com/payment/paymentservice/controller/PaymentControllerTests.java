package com.payment.paymentservice.controller;

import tools.jackson.databind.ObjectMapper;
import com.payment.paymentservice.dto.PaymentRequest;
import com.payment.paymentservice.dto.PaymentResponse;
import com.payment.paymentservice.dto.PaymentStatusUpdateRequest;
import com.payment.paymentservice.exception.PaymentNotFoundException;
import com.payment.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void createPayment_shouldReturnCreated() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id("test-id")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentService.createPayment(any(PaymentRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("test-id"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createPayment_shouldReturnBadRequest_whenInvalidRequest() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .amount(BigDecimal.valueOf(-10.00)) // Negative amount is invalid
                .currency("") // Blank currency is invalid
                .paymentMethod("")
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPaymentById_shouldReturnOk_whenPaymentExists() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id("test-id")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.getPaymentById("test-id")).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/test-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-id"))
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    void getPaymentById_shouldReturnNotFound_whenPaymentDoesNotExist() throws Exception {
        when(paymentService.getPaymentById("non-existent")).thenThrow(new PaymentNotFoundException("Payment not found"));

        mockMvc.perform(get("/api/v1/payments/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllPayments_shouldReturnOk() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id("test-id")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.getAllPayments(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("test-id"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void updatePaymentStatus_shouldReturnOk() throws Exception {
        PaymentStatusUpdateRequest request = PaymentStatusUpdateRequest.builder()
                .status("COMPLETED")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id("test-id")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentService.updatePaymentStatus(eq("test-id"), eq("COMPLETED"))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/payments/test-id/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-id"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void updatePaymentStatus_shouldReturnBadRequest_whenStatusIsBlank() throws Exception {
        PaymentStatusUpdateRequest request = PaymentStatusUpdateRequest.builder()
                .status("")
                .build();

        mockMvc.perform(patch("/api/v1/payments/test-id/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
