package com.mirror.product.mapper;

import com.mirror.product.dto.ComponentOptionalRequest;
import com.mirror.product.dto.ComponentOptionalResponse;
import com.mirror.product.entity.ComponentOptional;
import com.mirror.product.entity.Component;
import org.springframework.stereotype.Service;

@Service
public class ComponentOptionalMapper {

    public ComponentOptional toEntity(ComponentOptionalRequest request) {
        if (request == null) {
            return null;
        }
        
        ComponentOptional componentOptional = new ComponentOptional();
        componentOptional.setComponentOptionalName(request.getComponentOptionalName());
        componentOptional.setDescription(request.getDescription());
        
        if (request.getComponentId() != null) {
            Component component = new Component();
            component.setId(request.getComponentId());
            componentOptional.setComponent(component);
        }
        
        return componentOptional;
    }

    public ComponentOptionalResponse toResponse(ComponentOptional entity) {
        if (entity == null) {
            return null;
        }
        
        return ComponentOptionalResponse.builder()
                .id(entity.getId())
                .componentOptionalName(entity.getComponentOptionalName())
                .description(entity.getDescription())
                .componentId(entity.getComponent() != null ? entity.getComponent().getId() : null)
                .componentName(entity.getComponent() != null ? entity.getComponent().getComponentName() : null)
                .componentDescription(entity.getComponent() != null ? entity.getComponent().getDescription() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isActive(entity.getIsActive())
                .build();
    }

    public void updateEntityFromRequest(ComponentOptional entity, ComponentOptionalRequest request) {
        if (entity == null || request == null) {
            return;
        }
        
        if (request.getComponentOptionalName() != null) {
            entity.setComponentOptionalName(request.getComponentOptionalName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getComponentId() != null) {
            Component component = new Component();
            component.setId(request.getComponentId());
            entity.setComponent(component);
        }
    }
}