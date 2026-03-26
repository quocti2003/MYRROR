package com.mirror.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPaymentTransactionResponse {
    private String id;
    private String scheduleId;
    private String orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private Instant paidAt;
    private String recordedBy;
    private String notes;
    private Instant createdAt;
}
