package com.mirror.product.dto.sourcing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Material Aggregation Summary DTO
 *
 * Aggregated material requirements across all orders in the sourcing report.
 * Groups metals by type/purity and stones by type/shape for easier sourcing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialAggregationSummary {

    // Aggregated metal requirements
    private List<MetalAggregation> metals;

    // Aggregated stone requirements
    private List<StoneAggregation> stones;

    // Total estimated costs
    private BigDecimal totalMetalCost;
    private BigDecimal totalStoneCost;
    private BigDecimal totalLaborCost;
    private BigDecimal grandTotal;

    /**
     * Metal Aggregation - groups metals by type and purity
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetalAggregation {
        private String metalType;       // GOLD, PLATINUM, SILVER
        private String purity;          // 18K, 14K, etc.
        private String color;           // YELLOW, WHITE, ROSE
        private BigDecimal totalWeight; // Total grams needed (including loss)
        private Integer orderCount;     // Number of orders requiring this
        private BigDecimal estimatedCost;
    }

    /**
     * Stone Aggregation - groups stones by type and shape
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoneAggregation {
        private String stoneType;           // DIAMOND, RUBY, SAPPHIRE
        private String shape;               // ROUND, OVAL, PEAR
        private String sizeRange;           // e.g., "1.5mm-2.0mm"
        private String colorCategory;       // For colored stones
        private String clarityGrade;        // VS1, VS2, etc.
        private BigDecimal totalCaratWeight;
        private Integer totalQuantity;      // Total number of stones
        private Integer orderCount;         // Number of orders requiring this
        private BigDecimal estimatedCost;
    }
}
