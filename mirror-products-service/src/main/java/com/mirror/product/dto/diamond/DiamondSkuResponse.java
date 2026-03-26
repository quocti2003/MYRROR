package com.mirror.product.dto.diamond;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for diamond SKU generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiamondSkuResponse {

    private String skuCode;
    private String message;
    private boolean isUnique;

    // SKU breakdown for verification
    private String diamondType;
    private String colorClarity;
    private String carat;
    private String shape;
    private String origin;
    private String manufacturer;
    private String certShort;
}
