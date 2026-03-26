package com.mirror.product.dto.productionorder;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for bulk assignment operation
 * Used by P3-03: Bulk Partner Assignment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkAssignResponse {

    private String orderId;
    private String orderNumber;
    private int totalRequested;
    private int successCount;
    private int failedCount;
    private List<AssignmentResult> results;
    private String message;

    /**
     * Result for individual stage assignment
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AssignmentResult {
        private String stageId;
        private String stageName;
        private Integer stageOrder;
        private String vendorId;
        private String vendorName;
        private boolean success;
        private String errorMessage;
        private java.math.BigDecimal estimatedCost;
    }
}
