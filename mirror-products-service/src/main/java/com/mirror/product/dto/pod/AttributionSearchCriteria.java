package com.mirror.product.dto.pod;

import com.mirror.product.enums.AttributionStatus;
import com.mirror.product.enums.AttributionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributionSearchCriteria {

    private String orderId;
    private String podId;
    private String partnerId;
    private String qrCodeId;
    private AttributionType attributionType;
    private AttributionStatus status;
    private Instant orderPlacedAfter;
    private Instant orderPlacedBefore;
    private BigDecimal minOrderAmount;
    private BigDecimal maxOrderAmount;
    private Integer minDaysToConversion;
    private Integer maxDaysToConversion;
    private Boolean hasCommission;
}
