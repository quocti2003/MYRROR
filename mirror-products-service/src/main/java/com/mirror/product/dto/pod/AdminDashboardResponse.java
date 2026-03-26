package com.mirror.product.dto.pod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dashboard data for admin overview.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    // Partner stats
    private Long totalPartners;
    private Long activePartners;
    private Long pendingPartners;
    private Long suspendedPartners;

    // POD stats
    private Long totalPods;
    private Long activePods;
    private Long maintenancePods;
    private Long inactivePods;

    // QR Code stats
    private Long totalQrCodes;
    private Long activeQrCodes;

    // Scan stats
    private Long totalScans;
    private Long scansToday;
    private Long scansThisWeek;
    private Long scansThisMonth;
    private Double scanGrowthPercent;

    // Attribution stats
    private Long totalAttributions;
    private Long confirmedAttributions;
    private Long pendingAttributions;
    private BigDecimal totalAttributedRevenue;
    private BigDecimal attributedRevenueThisMonth;
    private Double overallConversionRate;

    // Commission stats
    private Long totalCommissions;
    private Long pendingCommissions;
    private Long approvedCommissions;
    private Long paidCommissions;
    private BigDecimal totalCommissionAmount;
    private BigDecimal pendingCommissionAmount;
    private BigDecimal paidCommissionAmount;

    // Trends (last 30 days)
    private List<DailyStats> scanTrend;
    private List<DailyStats> attributionTrend;

    // Top performers
    private List<TopPartner> topPartners;
    private List<TopPod> topPods;
    private List<TopCity> topCities;

    // Geographic distribution
    private Map<String, Long> podsByCity;
    private Map<String, Long> scansByCity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStats {
        private LocalDate date;
        private Long count;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPartner {
        private String partnerId;
        private String businessName;
        private String tier;
        private Integer podCount;
        private Long scanCount;
        private Long attributionCount;
        private BigDecimal totalRevenue;
        private BigDecimal commissionEarned;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPod {
        private String podId;
        private String podName;
        private String partnerName;
        private String city;
        private Long scanCount;
        private Long attributionCount;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCity {
        private String city;
        private Integer podCount;
        private Long scanCount;
        private Long attributionCount;
    }

    /**
     * Lightweight summary for quick dashboard view.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long totalPartners;
        private Long activePartners;
        private Long totalPods;
        private Long activePods;
        private Long totalQrCodes;
        private Long activeQrCodes;
        private Long totalScans;
        private Long scansToday;
        private Long totalAttributions;
        private BigDecimal totalAttributedRevenue;
        private Double overallConversionRate;
        private BigDecimal pendingCommissionAmount;
        private BigDecimal paidCommissionAmount;
    }
}
