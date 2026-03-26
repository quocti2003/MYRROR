package com.mirror.product.dto.sku;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GeneratedSkuListResponse {
    int page;
    int size;
    long totalElements;
    int totalPages;
    List<GeneratedSkuResponse> codes;
}
