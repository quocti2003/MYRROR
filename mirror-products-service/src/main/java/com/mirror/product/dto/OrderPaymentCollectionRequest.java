package com.mirror.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class OrderPaymentCollectionRequest {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amountPaid;

    private Instant paidAt;

    private String paymentMethod;

    private String note;

    private String recordedBy;
}
