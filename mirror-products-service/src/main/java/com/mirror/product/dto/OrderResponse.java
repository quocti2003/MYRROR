package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.dto.ProductResponse;
import com.mirror.product.enums.OrderStatus;
import com.mirror.product.enums.PaymentStatus;
import com.mirror.product.enums.PaymentTermType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private String id;
    private String userId;
    private String productId;
    private String vendorId;
    private VendorResponse vendor;
    private ProductResponse product;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private PaymentTermType paymentTermsType;
    private String paymentTerms;
    private String configuration;
    private String customerNotes;
    private BigDecimal paymentOutstanding;
    private BigDecimal subtotalAmount;
    private BigDecimal totalAmount;
    private String currency;
    private Integer quantity;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String sourceChannel;
    private String notes;
    private Instant expectedDeliveryDate;
    private Instant placedAt;
    private Instant lastStatusUpdatedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean overdue;

    private List<OrderItemResponse> items;
    private List<OrderPaymentScheduleResponse> paymentSchedule;
    private List<OrderStatusHistoryResponse> statusHistory;
}
