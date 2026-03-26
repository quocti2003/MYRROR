package com.mirror.product.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompleteRequest {

    @NotNull(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "Final payment received flag is required")
    private Boolean finalPaymentReceived;

    private BigDecimal finalPaymentAmount;

    private LocalDateTime finalPaymentDate;

    private String paymentMethod;

    private String notes;
}
