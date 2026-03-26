package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.PartnerInventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerInventoryResponse {

    private String id;
    private String partnerId;
    private String productId;
    private String productName;
    private Integer quantityOnHand;
    private Integer quantityReserved;
    private Integer quantityAvailable;
    private BigDecimal wholesalePrice;
    private BigDecimal partnerRetailPrice;
    private BigDecimal mirrorRetailPrice;
    private BigDecimal effectiveRetailPrice;
    private BigDecimal profitPerUnit;
    private BigDecimal marginPercent;
    private Integer reorderLevel;
    private Integer maxStockLevel;
    private boolean lowStock;
    private Instant lastRestockedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static PartnerInventoryResponse fromEntity(PartnerInventory entity) {
        if (entity == null) return null;
        return PartnerInventoryResponse.builder()
                .id(entity.getId())
                .partnerId(entity.getPartnerId())
                .productId(entity.getProductId())
                .productName(entity.getProduct() != null ? entity.getProduct().getItemName() : null)
                .quantityOnHand(entity.getQuantityOnHand())
                .quantityReserved(entity.getQuantityReserved())
                .quantityAvailable(entity.getQuantityAvailable())
                .wholesalePrice(entity.getWholesalePrice())
                .partnerRetailPrice(entity.getPartnerRetailPrice())
                .mirrorRetailPrice(entity.getMirrorRetailPrice())
                .effectiveRetailPrice(entity.getEffectiveRetailPrice())
                .profitPerUnit(entity.getProfitPerUnit())
                .marginPercent(entity.getMarginPercent())
                .reorderLevel(entity.getReorderLevel())
                .maxStockLevel(entity.getMaxStockLevel())
                .lowStock(entity.isLowStock())
                .lastRestockedAt(entity.getLastRestockedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
