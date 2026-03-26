package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.OrderStatus;
import com.mirror.product.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderSummaryResponse {

    private String id;
    private String userId;
    private String productId;
    private String vendorId;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private BigDecimal paymentOutstanding;
    private String currency;
    private Integer quantity;
    private String customerName;
    private String customerPhone;
    private Instant placedAt;
    private Instant lastStatusUpdatedAt;
    private Instant createdAt;
}
