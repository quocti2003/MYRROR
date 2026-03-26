package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.PodUserAttribution;
import com.mirror.product.enums.PodUserAttributionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodUserAttributionResponse {

    private String id;
    private Long userId;
    private String podId;
    private String partnerId;
    private String productId;
    private String qrCodeId;
    private Instant firstScanAt;
    private Instant expiresAt;
    private PodUserAttributionStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static PodUserAttributionResponse fromEntity(PodUserAttribution entity) {
        if (entity == null) {
            return null;
        }
        return PodUserAttributionResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .podId(entity.getPodId())
                .partnerId(entity.getPartnerId())
                .productId(entity.getProductId())
                .qrCodeId(entity.getQrCodeId())
                .firstScanAt(entity.getFirstScanAt())
                .expiresAt(entity.getExpiresAt())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
