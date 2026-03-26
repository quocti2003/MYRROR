package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.PodQrCode;
import com.mirror.product.enums.QrCodeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeResponse {

    private String id;
    private String podId;
    private String podName;
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

    public static QrCodeResponse fromEntity(PodQrCode entity) {
        if (entity == null) {
            return null;
        }
        return QrCodeResponse.builder()
                .id(entity.getId())
                .podId(entity.getPodId())
                .podName(entity.getPod() != null ? entity.getPod().getName() : null)
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
}
