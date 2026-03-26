package com.mirror.product.dto;

import com.mirror.product.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {

    @NotNull
    private OrderStatus status;

    private String note;

    private String changedBy;
}
