package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.OrderStatus;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderStatusHistoryResponse {

    private String id;
    private OrderStatus status;
    private String note;
    private String changedBy;
    private Instant changedAt;
}
