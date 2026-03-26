package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.entity.PurchaseOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for Purchase Order
 * Migrated from mirror-mrp-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseOrderResponse {

    private UUID id;
    private String vendorId;
    private String vendorName;
    private String vendorCountry;
    private String status;
    private LocalDateTime createdAt;
    private LocalDate expectedDeliveryDate;
    private BigDecimal totalCost;
    private Integer itemCount;
    private List<PurchaseOrderItemResponse> items;

    public PurchaseOrderResponse(PurchaseOrder purchaseOrder) {
        this.id = purchaseOrder.getId();
        this.status = purchaseOrder.getStatus();
        this.createdAt = purchaseOrder.getCreatedAt();
        this.expectedDeliveryDate = purchaseOrder.getExpectedDeliveryDate();
        this.totalCost = purchaseOrder.getTotalCost();

        if (purchaseOrder.getVendor() != null) {
            this.vendorId = purchaseOrder.getVendor().getId();
            this.vendorName = purchaseOrder.getVendor().getName();
            this.vendorCountry = purchaseOrder.getVendor().getCountry();
        }

        if (purchaseOrder.getPoItems() != null) {
            this.itemCount = purchaseOrder.getPoItems().size();
            this.items = purchaseOrder.getPoItems().stream()
                    .map(PurchaseOrderItemResponse::new)
                    .collect(Collectors.toList());
        }
    }

    public PurchaseOrderResponse(PurchaseOrder purchaseOrder, boolean includeItems) {
        this.id = purchaseOrder.getId();
        this.status = purchaseOrder.getStatus();
        this.createdAt = purchaseOrder.getCreatedAt();
        this.expectedDeliveryDate = purchaseOrder.getExpectedDeliveryDate();
        this.totalCost = purchaseOrder.getTotalCost();

        if (purchaseOrder.getVendor() != null) {
            this.vendorId = purchaseOrder.getVendor().getId();
            this.vendorName = purchaseOrder.getVendor().getName();
            this.vendorCountry = purchaseOrder.getVendor().getCountry();
        }

        if (purchaseOrder.getPoItems() != null) {
            this.itemCount = purchaseOrder.getPoItems().size();
        } else {
            this.itemCount = 0;
        }

        if (includeItems) {
            this.items = purchaseOrder.getPoItems() != null
                    ? purchaseOrder.getPoItems().stream()
                            .map(PurchaseOrderItemResponse::new)
                            .collect(Collectors.toList())
                    : List.of();
        }
    }
}
