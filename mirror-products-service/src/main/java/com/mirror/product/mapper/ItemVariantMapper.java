package com.mirror.product.mapper;

import com.mirror.product.dto.ItemVariantRequest;
import com.mirror.product.dto.ItemVariantResponse;
import com.mirror.product.entity.ItemVariant;
import org.springframework.stereotype.Service;

@Service
public class ItemVariantMapper {

    public ItemVariant toEntity(ItemVariantRequest request) {
        if (request == null) {
            return null;
        }
        
        ItemVariant itemVariant = new ItemVariant();
        itemVariant.setItemVariantUrl(request.getItemVariantUrl());
        itemVariant.setDescription(request.getDescription());
        return itemVariant;
    }

    public ItemVariantResponse toResponse(ItemVariant entity) {
        if (entity == null) {
            return null;
        }
        
        return ItemVariantResponse.builder()
                .id(entity.getId())
                .itemVariantUrl(entity.getItemVariantUrl())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isActive(entity.getIsActive())
                .build();
    }

    public void updateEntityFromRequest(ItemVariant entity, ItemVariantRequest request) {
        if (entity == null || request == null) {
            return;
        }
        
        if (request.getItemVariantUrl() != null) {
            entity.setItemVariantUrl(request.getItemVariantUrl());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
    }
}