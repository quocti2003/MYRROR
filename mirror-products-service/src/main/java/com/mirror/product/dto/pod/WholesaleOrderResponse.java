package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.WholesaleOrder;
import com.mirror.product.enums.WholesaleOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WholesaleOrderResponse {

    private String id;
    private String orderNumber;
    private String partnerId;
    private String partnerName;
    private WholesaleOrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String shippingAddress;
    private String shippingMethod;
    private BigDecimal shippingCost;
    private String trackingNumber;
    private String paymentMethod;
    private String paymentReference;
    private String paymentStatus;
    private Instant submittedAt;
    private Instant approvedAt;
    private String approvedBy;
    private Instant shippedAt;
    private Instant deliveredAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private String notes;
    private List<WholesaleOrderItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;

    public static WholesaleOrderResponse fromEntity(WholesaleOrder entity) {
        if (entity == null) return null;
        WholesaleOrderResponseBuilder builder = WholesaleOrderResponse.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .partnerId(entity.getPartnerId())
                .partnerName(entity.getPartner() != null ? entity.getPartner().getBusinessName() : null)
                .status(entity.getStatus())
                .subtotal(entity.getSubtotal())
                .discountAmount(entity.getDiscountAmount())
                .taxAmount(entity.getTaxAmount())
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .shippingAddress(entity.getShippingAddress())
                .shippingMethod(entity.getShippingMethod())
                .shippingCost(entity.getShippingCost())
                .trackingNumber(entity.getTrackingNumber())
                .paymentMethod(entity.getPaymentMethod())
                .paymentReference(entity.getPaymentReference())
                .paymentStatus(entity.getPaymentStatus())
                .submittedAt(entity.getSubmittedAt())
                .approvedAt(entity.getApprovedAt())
                .approvedBy(entity.getApprovedBy())
                .shippedAt(entity.getShippedAt())
                .deliveredAt(entity.getDeliveredAt())
                .completedAt(entity.getCompletedAt())
                .cancelledAt(entity.getCancelledAt())
                .cancellationReason(entity.getCancellationReason())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        if (entity.getItems() != null) {
            builder.items(entity.getItems().stream()
                    .map(WholesaleOrderItemResponse::fromEntity)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
