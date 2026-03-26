package com.mirror.product.dto.sku;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuSearchResponse {
    private String query;
    private int totalResults;
    private List<SearchResultItem> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResultItem {
        private String skuCode;
        private String itemName;
        private String description;
        private String category;
        private Double price;
        private double relevance;
    }
}
