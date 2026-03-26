package com.mirror.product.mapper;

import com.mirror.product.dto.ComponentRequest;
import com.mirror.product.dto.ComponentResponse;
import com.mirror.product.entity.Component;
import com.mirror.product.entity.MirrorProduct;
import org.springframework.stereotype.Service;

@Service
public class ComponentMapper {

    public Component toEntity(ComponentRequest request) {
        if (request == null) {
            return null;
        }

        Component component = new Component();
        component.setComponentName(request.getComponentName());
        component.setDescription(request.getDescription());

        if (request.getProductId() != null) {
            MirrorProduct product = new MirrorProduct();
            product.setId(request.getProductId());
            component.setProduct(product);
        }

        return component;
    }

    public ComponentResponse toResponse(Component entity) {
        if (entity == null) {
            return null;
        }

        return ComponentResponse.builder()
                .id(entity.getId())
                .componentName(entity.getComponentName())
                .description(entity.getDescription())
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .productName(entity.getProduct() != null ? entity.getProduct().getItemName() : null)
                .productDescription(entity.getProduct() != null ? entity.getProduct().getDescription() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isActive(entity.getIsActive())
                .build();
    }

    public void updateEntityFromRequest(Component entity, ComponentRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getComponentName() != null) {
            entity.setComponentName(request.getComponentName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getProductId() != null) {
            MirrorProduct product = new MirrorProduct();
            product.setId(request.getProductId());
            entity.setProduct(product);
        }
    }
}
