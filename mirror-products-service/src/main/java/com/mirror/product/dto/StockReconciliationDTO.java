package com.mirror.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class StockReconciliationDTO {

    /**
     * Request DTO for creating a new stock reconciliation record
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String documentNumber;
        private String warehouseId;
        private String warehouseName;
        private LocalDateTime reconciliationDate;
        private Summary summary;
        private List<ReconciliationItem> items;
        private String notes;
    }

    /**
     * Summary statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer total;
        private Integer match;
        private Integer missing;
        private Integer excess;
        private Integer notInMisa;
    }

    /**
     * Individual item in the reconciliation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationItem {
        private String sku;
        private String name;
        private Integer physicalCount;
        private Integer misaCount;
        private Integer difference;
        private String status; // match, missing, excess, not_in_misa
        private Double unitPrice;
        private String unit;
        private String quality;
    }

    /**
     * Response DTO for stock reconciliation record
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String documentNumber;
        private String warehouseId;
        private String warehouseName;
        private LocalDateTime reconciliationDate;
        private String createdBy;
        private String createdByName;
        private Summary summary;
        private Integer totalItems;
        private Integer matchedItems;
        private Integer missingItems;
        private Integer excessItems;
        private Integer notInSystemItems;
        private String reportFileName;
        private String reportFileUrl;
        private String notes;
        private Instant createdAt;
        private Instant updatedAt;
    }

    /**
     * Response DTO with full items data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailResponse {
        private String id;
        private String documentNumber;
        private String warehouseId;
        private String warehouseName;
        private LocalDateTime reconciliationDate;
        private String createdBy;
        private String createdByName;
        private Summary summary;
        private List<ReconciliationItem> items;
        private Integer totalItems;
        private Integer matchedItems;
        private Integer missingItems;
        private Integer excessItems;
        private Integer notInSystemItems;
        private String reportFileName;
        private String reportFileUrl;
        private String notes;
        private Instant createdAt;
        private Instant updatedAt;
    }

    /**
     * Response DTO for list queries
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> records;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }

    /**
     * Response after creating a record
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResponse {
        private String id;
        private String documentNumber;
        private String reportFileUrl;
        private String reportFileName;
        private String message;
    }

    /**
     * Download URL response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadResponse {
        private String downloadUrl;
        private String fileName;
        private Long expiresIn; // seconds
    }
}
