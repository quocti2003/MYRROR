package com.mirror.product.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmRequest {

    @NotNull(message = "Order ID is required")
    private String orderId;

    private String notes;
}
