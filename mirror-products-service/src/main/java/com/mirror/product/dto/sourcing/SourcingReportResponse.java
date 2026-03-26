package com.mirror.product.dto.sourcing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sourcing Report Response DTO
 *
 * Contains a per-partner sourcing report with all assigned production orders
 * and their JTRC specifications, plus material aggregation summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourcingReportResponse {

    private String reportId;
    private String productionPlanId;
    private String productionPlanName;

    // Partner (Vendor) information
    private String vendorId;
    private String vendorName;
    private String vendorCode;
    private String vendorEmail;
    private String vendorPhone;
    private String vendorCountry;

    // Report metadata
    private LocalDateTime generatedAt;
    private String generatedBy;
    private LocalDate targetStartDate;
    private LocalDate targetEndDate;

    // Assigned orders for this vendor
    private List<SourcingOrderItem> orders;

    // Material aggregation summary
    private MaterialAggregationSummary materialSummary;

    // Totals
    private Integer totalOrders;
    private Integer totalQuantity;
    private BigDecimal estimatedTotalCost;

    // Send status
    private String sendStatus; // DRAFT, SENT, ACKNOWLEDGED
    private LocalDateTime sentAt;
    private String sentTo;
}
