package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.OrderStatus;
import com.mirror.product.enums.PaymentScheduleStatus;
import com.mirror.product.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderPaymentScheduleResponse {

    private String id;
    private String orderId;
    private OrderStatus orderStatus;
    private PaymentStatus orderPaymentStatus;
    private String currency;
    private BigDecimal orderTotalAmount;
    private Instant dueDate;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private BigDecimal outstanding;
    private PaymentScheduleStatus status;
    private Instant paidAt;
    private String paymentMethod;
    private String notes;
    private String customerName;
    private String customerPhone;
    private Instant createdAt;
}
