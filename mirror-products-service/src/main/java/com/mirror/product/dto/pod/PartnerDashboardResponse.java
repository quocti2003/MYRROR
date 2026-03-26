package com.mirror.product.dto.pod;

import com.mirror.product.enums.PartnerStatus;
import com.mirror.product.enums.PartnerTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard data for a partner's portal view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerDashboardResponse {

    // Partner info
    private String partnerId;
    private String businessName;
    private PartnerStatus status;
    private PartnerTier tier;
    private BigDecimal commissionRate;

    // Summary stats
    private Integer totalPods;
    private Integer activePods;
    private Integer totalQrCodes;
    private Integer activeQrCodes;

    // Scan stats
    private Long totalScans;
    private Long uniqueScans;
    private Long scansThisMonth;
    private Long scansLastMonth;
    private Double scanGrowthPercent;

    // Attribution stats
    private Long totalAttributions;
    private Long attributionsThisMonth;
    private BigDecimal totalAttributedAmount;
    private BigDecimal attributedAmountThisMonth;
    private Double conversionRate;

    // Commission stats
    private BigDecimal totalEarned;
    private BigDecimal pendingCommission;
    private BigDecimal lastMonthCommission;

    // Recent activity
    private List<RecentScan> recentScans;
    private List<RecentAttribution> recentAttributions;

    // Top performing
    private List<TopPod> topPods;
    private List<TopProduct> topProducts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentScan {
        private String scanId;
        private String podName;
        private String productName;
        private String city;
        private String deviceType;
        private Instant scannedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentAttribution {
        private String attributionId;
        private String orderId;
        private String podName;
        private BigDecimal orderAmount;
        private BigDecimal attributedAmount;
        private Instant orderPlacedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPod {
        private String podId;
        private String podName;
        private String locationName;
        private Long scanCount;
        private Long attributionCount;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProduct {
        private String productId;
        private String productName;
        private String productSku;
        private Long scanCount;
        private Long attributionCount;
    }
}
