package com.mirror.product.mapper;

import com.mirror.product.dto.SkuRequest;
import com.mirror.product.dto.SkuResponse;
import com.mirror.product.entity.MirrorProduct;
import org.springframework.stereotype.Component;

@Component
public class SkuMapper {

    public MirrorProduct toEntity(SkuRequest request) {
        if (request == null) {
            return null;
        }

        MirrorProduct product = new MirrorProduct();
        product.setItemName(request.getSkuName());
        product.setDescription(request.getDescription());
        product.setSkuCode(normalizeSkuCode(request.getSkuCode()));
        return product;
    }

    public SkuResponse toResponse(MirrorProduct entity) {
        if (entity == null) {
            return null;
        }

        return SkuResponse.builder()
                .id(entity.getId())
                .skuName(entity.getItemName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .isActive(entity.getIsActive())
                .skuCode(entity.getSkuCode())
                .misaCategoryId(entity.getMisaCategoryId())
                .misaCategoryCode(entity.getMisaCategoryCode())
                .misaCategoryName(entity.getMisaCategoryName())
                .misaLastSyncedAt(entity.getMisaLastSyncedAt())
                .build();
    }

    public void updateEntityFromRequest(MirrorProduct entity, SkuRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getSkuName() != null) {
            entity.setItemName(request.getSkuName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getSkuCode() != null) {
            entity.setSkuCode(normalizeSkuCode(request.getSkuCode()));
        }
    }

    private String normalizeSkuCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }
}
