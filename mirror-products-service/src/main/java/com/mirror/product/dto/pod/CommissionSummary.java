package com.mirror.product.dto.pod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Summary statistics for commissions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSummary {

    // Overall stats
    private long totalCommissions;
    private long pendingCommissions;
    private long approvedCommissions;
    private long paidCommissions;
    private long cancelledCommissions;

    // Amount stats
    private BigDecimal totalPendingAmount;
    private BigDecimal totalApprovedAmount;
    private BigDecimal totalPaidAmount;
    private BigDecimal totalCancelledAmount;

    // Partner specific (optional)
    private String partnerId;
    private String partnerName;
    private BigDecimal partnerTotalEarned;
    private BigDecimal partnerPendingAmount;
    private Integer partnerTotalOrders;
}
