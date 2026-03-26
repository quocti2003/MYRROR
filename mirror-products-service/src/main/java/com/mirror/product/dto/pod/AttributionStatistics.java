package com.mirror.product.dto.pod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Statistics for attribution performance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributionStatistics {

    private String partnerId;
    private String partnerName;
    private String podId;
    private String podName;

    // Counts
    private long totalAttributions;
    private long confirmedAttributions;
    private long pendingAttributions;
    private long cancelledAttributions;

    // Amounts
    private BigDecimal totalOrderAmount;
    private BigDecimal totalAttributedAmount;
    private BigDecimal averageOrderAmount;

    // Performance
    private Double conversionRate;
    private Double averageDaysToConversion;
    private Integer minDaysToConversion;
    private Integer maxDaysToConversion;

    // Time range
    private Instant startDate;
    private Instant endDate;
}
