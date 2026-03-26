package com.mirror.product.dto.sku;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for SKU generation.
 *
 * New model (simplified):
 * - code = descriptive product specs (e.g., RNG-18KWG-W-2.09-LG-RD-1.29-N)
 * - barcode = unique identifier, same as skuCode (e.g., MIR2512301234567)
 * - productId = skuCode (barcode-based)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuGenerationResponse {
    private String code;                // Descriptive code (RNG-18KWG-W-2.09-LG-RD-1.29-N)
    private String barcode;             // Unique barcode identifier (same as skuCode)
    private String description;
    private int length;
    private boolean truncated;
    private String originalLength;
    private String productId;           // The skuCode (barcode-based)
}
