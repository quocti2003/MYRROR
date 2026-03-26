package com.mirror.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class OrderPaymentScheduleRequest {

    @NotNull
    private Instant dueDate;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal amountDue;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal amountPaid;

    private String paymentMethod;

    private String notes;
}
