package com.mirror.product.dto.pod;

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
public class PhygitalDashboardResponse {

    // Inventory stats
    private long totalInventoryItems;
    private BigDecimal totalInventoryValue;
    private long lowStockCount;

    // Sales stats - this month
    private long salesCountThisMonth;
    private BigDecimal revenueThisMonth;
    private BigDecimal profitThisMonth;
    private BigDecimal avgMarginThisMonth;

    // Sales stats - last month
    private long salesCountLastMonth;
    private BigDecimal revenueLastMonth;
    private BigDecimal profitLastMonth;

    // Growth
    private BigDecimal revenueGrowthPercent;
    private BigDecimal profitGrowthPercent;

    // Wholesale orders
    private long pendingWholesaleOrders;
    private long inTransitWholesaleOrders;
    private BigDecimal totalPurchased;

    // Lists
    private List<PartnerInventoryResponse> lowStockAlerts;
    private List<PartnerSaleResponse> recentSales;
}
