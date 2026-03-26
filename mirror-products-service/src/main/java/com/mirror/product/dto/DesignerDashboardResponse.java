package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DesignerDashboardResponse {

    // Designer basic info
    private String designerId;
    private String designerName;
    private String brandName;

    // Overall statistics
    private BigDecimal totalSalesAmount;
    private Integer totalSalesCount;
    private BigDecimal totalEarnings;
    private BigDecimal totalCommissionEarned;
    private BigDecimal totalLoyaltyEarned;
    private Integer activeDesignsCount;
    private Integer totalDesignsCreated;

    // Monthly statistics
    private BigDecimal currentMonthSales;
    private BigDecimal currentMonthEarnings;
    private Integer currentMonthSalesCount;

    // Performance metrics
    private BigDecimal averageRating;
    private Integer totalReviewsCount;
    private BigDecimal averageOrderValue;

    // Recent activity
    private List<DesignSaleTransactionResponse> recentSales;
    private List<DesignProductResponse> topDesigns;
    private List<DesignProductResponse> activeCommissions;

    // Helper method to calculate total earnings
    public BigDecimal getTotalEarnings() {
        BigDecimal commission = totalCommissionEarned != null ? totalCommissionEarned : BigDecimal.ZERO;
        BigDecimal loyalty = totalLoyaltyEarned != null ? totalLoyaltyEarned : BigDecimal.ZERO;
        return commission.add(loyalty);
    }
}