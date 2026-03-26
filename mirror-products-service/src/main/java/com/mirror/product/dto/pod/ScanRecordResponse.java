package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.PodQrScan;
import com.mirror.product.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanRecordResponse {

    private String id;
    private String qrCodeId;
    private String shortCode;
    private String podId;
    private String podName;
    private String productId;
    private String productName;
    private String partnerId;
    private String partnerName;
    private String sessionId;
    private Long userId;
    private String ipAddress;
    private String userAgent;
    private DeviceType deviceType;
    private String os;
    private String browser;
    private String referer;
    private String country;
    private String city;
    private Instant scannedAt;
    private Boolean isUnique;
    private Instant createdAt;

    public static ScanRecordResponse fromEntity(PodQrScan entity) {
        if (entity == null) {
            return null;
        }
        return ScanRecordResponse.builder()
                .id(entity.getId())
                .qrCodeId(entity.getQrCodeId())
                .shortCode(entity.getQrCode() != null ? entity.getQrCode().getShortCode() : null)
                .podId(entity.getPodId())
                .podName(entity.getPod() != null ? entity.getPod().getName() : null)
                .productId(entity.getProductId())
                .productName(entity.getProduct() != null ? entity.getProduct().getItemName() : null)
                .partnerId(entity.getPartnerId())
                .partnerName(entity.getPartner() != null ? entity.getPartner().getBusinessName() : null)
                .sessionId(entity.getSessionId())
                .userId(entity.getUserId())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .deviceType(entity.getDeviceType())
                .os(entity.getOs())
                .browser(entity.getBrowser())
                .referer(entity.getReferer())
                .country(entity.getCountry())
                .city(entity.getCity())
                .scannedAt(entity.getScannedAt())
                .isUnique(entity.getIsUnique())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
