package com.mirror.product.dto.sku;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class GeneratedSkuResponse {
    String skuCode;           // Unique barcode identifier (e.g., MIR2512301234567)
    String descriptiveCode;   // Human-readable product specs (e.g., RNG-18KWG-W-2.09-LG-RD-1.29-N)
    String barcode;           // Same as skuCode (kept for compatibility)
    String misaItemCode;
    String itemName;
    String category;
    String description;
    BigDecimal price;
    Integer currentQuantity;
    Integer minQuantity;
    Integer maxQuantity;
    String location;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
