package com.payment.paymentservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_stripe_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedStripeEvent {

    @Id
    private String eventId;

    private LocalDateTime processedAt;
}
