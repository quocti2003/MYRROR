package com.mirror.product.dto.sku;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BulkSkuGenerationResponse {
    int totalRows;
    int successCount;
    int failureCount;
    List<GeneratedSkuResponse> generatedCodes;
    List<String> errors;
}
