package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.PodAttribution;
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
public class AttributionResponse {

    private String id;
    private String orderId;
    private String podId;
    private String podName;
    private String partnerId;
    private String partnerName;
    private String qrCodeId;
    private String qrShortCode;
    private String scanId;
    private AttributionType attributionType;
    private BigDecimal attributionWeight;
    private BigDecimal orderAmount;
    private BigDecimal attributedAmount;
    private String currency;
    private Instant firstScanAt;
    private Instant lastScanAt;
    private Instant orderPlacedAt;
    private Integer daysToConversion;
    private Integer touchCount;
    private AttributionStatus status;
    private String commissionId;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public static AttributionResponse fromEntity(PodAttribution entity) {
        if (entity == null) {
            return null;
        }
        return AttributionResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .podId(entity.getPodId())
                .podName(entity.getPod() != null ? entity.getPod().getName() : null)
                .partnerId(entity.getPartnerId())
                .partnerName(entity.getPartner() != null ? entity.getPartner().getBusinessName() : null)
                .qrCodeId(entity.getQrCodeId())
                .qrShortCode(entity.getQrCode() != null ? entity.getQrCode().getShortCode() : null)
                .scanId(entity.getScanId())
                .attributionType(entity.getAttributionType())
                .attributionWeight(entity.getAttributionWeight())
                .orderAmount(entity.getOrderAmount())
                .attributedAmount(entity.getAttributedAmount())
                .currency(entity.getCurrency())
                .firstScanAt(entity.getFirstScanAt())
                .lastScanAt(entity.getLastScanAt())
                .orderPlacedAt(entity.getOrderPlacedAt())
                .daysToConversion(entity.getDaysToConversion())
                .touchCount(entity.getTouchCount())
                .status(entity.getStatus())
                .commissionId(entity.getCommissionId())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
