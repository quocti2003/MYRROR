package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.PodQrCode;
import com.mirror.product.enums.QrCodeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeDetailResponse {

    private String id;
    private String podId;
    private String podName;
    private String podLocationName;
    private String partnerId;
    private String partnerName;
    private String productId;
    private String productName;
    private String productSku;
    private String shortCode;
    private String fullUrl;
    private String qrImageUrl;
    private QrCodeStatus status;
    private Long scanCount;
    private Instant lastScannedAt;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Scan statistics
    private Long totalScans;
    private Long uniqueScans;
    private Long todayScans;
    private Long weekScans;
    private Long monthScans;

    // Recent scans
    private List<ScanSummary> recentScans;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanSummary {
        private String scanId;
        private Instant scannedAt;
        private String deviceType;
        private String city;
        private String country;
        private Boolean isUnique;
    }

    public static QrCodeDetailResponse fromEntity(PodQrCode entity) {
        if (entity == null) {
            return null;
        }
        return QrCodeDetailResponse.builder()
                .id(entity.getId())
                .podId(entity.getPodId())
                .podName(entity.getPod() != null ? entity.getPod().getName() : null)
                .podLocationName(entity.getPod() != null ? entity.getPod().getLocationName() : null)
                .partnerId(entity.getPod() != null ? entity.getPod().getPartnerId() : null)
                .partnerName(entity.getPod() != null && entity.getPod().getPartner() != null
                        ? entity.getPod().getPartner().getBusinessName() : null)
                .productId(entity.getProductId())
                .productName(entity.getProduct() != null ? entity.getProduct().getItemName() : null)
                .productSku(entity.getProduct() != null ? entity.getProduct().getSkuCode() : null)
                .shortCode(entity.getShortCode())
                .fullUrl(entity.getFullUrl())
                .qrImageUrl(entity.getQrImageUrl())
                .status(entity.getStatus())
                .scanCount(entity.getScanCount())
                .lastScannedAt(entity.getLastScannedAt())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public QrCodeDetailResponse withStatistics(
            Long totalScans,
            Long uniqueScans,
            Long todayScans,
            Long weekScans,
            Long monthScans
    ) {
        this.totalScans = totalScans;
        this.uniqueScans = uniqueScans;
        this.todayScans = todayScans;
        this.weekScans = weekScans;
        this.monthScans = monthScans;
        return this;
    }

    public QrCodeDetailResponse withRecentScans(List<ScanSummary> recentScans) {
        this.recentScans = recentScans;
        return this;
    }
}
