package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.WholesaleOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WholesaleOrderItemResponse {

    private String id;
    private String orderId;
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal discountPercent;
    private BigDecimal lineTotal;

    public static WholesaleOrderItemResponse fromEntity(WholesaleOrderItem entity) {
        if (entity == null) return null;
        return WholesaleOrderItemResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .productId(entity.getProductId())
                .productName(entity.getProduct() != null ? entity.getProduct().getItemName() : null)
                .quantity(entity.getQuantity())
                .retailPrice(entity.getRetailPrice())
                .wholesalePrice(entity.getWholesalePrice())
                .discountPercent(entity.getDiscountPercent())
                .lineTotal(entity.getLineTotal())
                .build();
    }
}
