package com.mirror.product.dto;

import com.mirror.product.enums.PaymentTermType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class OrderRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String productId;

    private String vendorId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal subtotalAmount;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal totalAmount;

    private String currency;

    @NotNull
    private PaymentTermType paymentTermsType;

    private String paymentTerms;

    private String configuration;

    private String customerNotes;

    private Integer quantity;

    private String customerName;

    private String customerEmail;

    private String customerPhone;

    private String sourceChannel;

    private Instant expectedDeliveryDate;

    private String notes;

    @Valid
    private List<OrderItemRequest> items;

    @Valid
    private List<OrderPaymentScheduleRequest> paymentSchedule;
}
