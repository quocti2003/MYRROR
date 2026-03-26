package com.mirror.product.dto.productionorder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.PartnerCapabilityType;
import com.mirror.product.enums.ProductionOrderStageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for production order cost estimation
 * Used by P3-04: Cost Estimation on Assignment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CostEstimateResponse {

    private String orderId;
    private String orderNumber;
    private Integer quantity;

    // Cost breakdown
    private List<StageCostBreakdown> stageBreakdown;

    // Totals
    private BigDecimal totalEstimatedCost;
    private BigDecimal totalActualCost;
    private int stagesWithEstimate;
    private int stagesWithoutEstimate;
    private int totalStages;

    // Summary
    private String currency;
    private String notes;

    /**
     * Cost breakdown for individual stage
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StageCostBreakdown {
        private String stageId;
        private Integer stageOrder;
        private String stageName;
        private PartnerCapabilityType requiredCapability;
        private ProductionOrderStageStatus status;

        // Vendor info
        private String assignedVendorId;
        private String assignedVendorName;

        // Costs
        private BigDecimal estimatedCost;
        private BigDecimal actualCost;
        private BigDecimal vendorCostPerPiece;
        private BigDecimal vendorCostPerGram;

        // Estimation source
        private String costSource; // "MANUAL", "VENDOR_RATE", "NOT_ASSIGNED"
        private boolean hasVendorAssigned;
    }
}
