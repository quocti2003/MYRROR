package com.mirror.product.dto.sku;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SkuSearchRequest {
    @NotBlank(message = "Query is required")
    private String query;

    private Double threshold = 0.3;  // Minimum similarity score (0-1)
    private Integer limit = 50;      // Maximum results to return
}
