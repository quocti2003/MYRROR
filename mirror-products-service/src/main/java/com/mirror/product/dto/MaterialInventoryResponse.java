package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.entity.MaterialInventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Material Inventory
 * Migrated from mirror-mrp-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MaterialInventoryResponse {

    private UUID id;
    private String name;
    private String type;
    private String vendorId;
    private String vendorName;
    private BigDecimal basePrice;
    private BigDecimal marketPrice;
    private String currency;
    private BigDecimal taxCustomsPercent;
    private BigDecimal assemblyCostLocal;
    private Integer deliveryLeadTimeDays;
    private Integer productionLeadTimeDays;
    private Integer assemblyLeadTimeDays;
    private LocalDateTime createdAt;
    private Integer purchaseOrderItemsCount;

    public MaterialInventoryResponse(MaterialInventory material) {
        this.id = material.getId();
        this.name = material.getName();
        this.type = material.getType();
        this.basePrice = material.getBasePrice();
        this.marketPrice = material.getMarketPrice();
        this.currency = material.getCurrency();
        this.taxCustomsPercent = material.getTaxCustomsPercent();
        this.assemblyCostLocal = material.getAssemblyCostLocal();
        this.deliveryLeadTimeDays = material.getDeliveryLeadTimeDays();
        this.productionLeadTimeDays = material.getProductionLeadTimeDays();
        this.assemblyLeadTimeDays = material.getAssemblyLeadTimeDays();
        this.createdAt = material.getCreatedAt();

        if (material.getVendor() != null) {
            this.vendorId = material.getVendor().getId();
            this.vendorName = material.getVendor().getName();
        }

        if (material.getPurchaseOrderItems() != null) {
            this.purchaseOrderItemsCount = material.getPurchaseOrderItems().size();
        }
    }
}
