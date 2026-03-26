package com.mirror.product.dto.sku;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkuGenerationResult {
    private String code;
    private boolean truncated;
}
