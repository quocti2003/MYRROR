package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.PartnerSaleItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerSaleItemResponse {

    private String id;
    private String saleId;
    private String productId;
    private String productName;
    private String inventoryId;
    private Integer quantity;
    private BigDecimal wholesaleCost;
    private BigDecimal sellingPrice;
    private BigDecimal lineTotal;
    private BigDecimal profit;

    public static PartnerSaleItemResponse fromEntity(PartnerSaleItem entity) {
        if (entity == null) return null;
        return PartnerSaleItemResponse.builder()
                .id(entity.getId())
                .saleId(entity.getSaleId())
                .productId(entity.getProductId())
                .productName(entity.getProduct() != null ? entity.getProduct().getItemName() : null)
                .inventoryId(entity.getInventoryId())
                .quantity(entity.getQuantity())
                .wholesaleCost(entity.getWholesaleCost())
                .sellingPrice(entity.getSellingPrice())
                .lineTotal(entity.getLineTotal())
                .profit(entity.getProfit())
                .build();
    }
}
