package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.entity.PurchaseOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for Purchase Order Item
 * Migrated from mirror-mrp-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseOrderItemResponse {

    private UUID id;
    private UUID purchaseOrderId;
    private UUID materialId;
    private String materialName;
    private String materialType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    public PurchaseOrderItemResponse(PurchaseOrderItem item) {
        this.id = item.getId();
        this.quantity = item.getQuantity();
        this.unitPrice = item.getUnitPrice();
        this.totalPrice = item.getTotalPrice();

        if (item.getPurchaseOrder() != null) {
            this.purchaseOrderId = item.getPurchaseOrder().getId();
        }

        if (item.getMaterial() != null) {
            this.materialId = item.getMaterial().getId();
            this.materialName = item.getMaterial().getName();
            this.materialType = item.getMaterial().getType();
        }
    }
}
