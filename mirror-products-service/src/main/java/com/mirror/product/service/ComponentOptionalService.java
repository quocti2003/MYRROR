package com.mirror.product.service;

import com.mirror.product.entity.ComponentOptional;
import com.mirror.product.entity.Component;
import com.mirror.product.repository.ComponentOptionalRepository;
import com.mirror.product.repository.ComponentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ComponentOptionalService extends BaseService<ComponentOptional, String> {
    
    private final ComponentOptionalRepository componentOptionalRepository;
    private final ComponentRepository componentRepository;
    
    public ComponentOptionalService(ComponentOptionalRepository componentOptionalRepository, ComponentRepository componentRepository) {
        super(componentOptionalRepository);
        this.componentOptionalRepository = componentOptionalRepository;
        this.componentRepository = componentRepository;
    }
    
    public List<ComponentOptional> findByComponentId(String componentId) {
        return componentOptionalRepository.findActiveByComponentId(componentId);
    }
    
    public Optional<ComponentOptional> findByComponentOptionalName(String componentOptionalName) {
        return componentOptionalRepository.findActiveByComponentOptionalName(componentOptionalName);
    }
    
    public boolean existsByComponentOptionalNameAndComponentId(String componentOptionalName, String componentId) {
        return componentOptionalRepository.existsActiveByComponentOptionalNameAndComponentId(componentOptionalName, componentId);
    }
    
    @Override
    public ComponentOptional update(String componentOptionalId, ComponentOptional componentOptionalDetails) {
        ComponentOptional componentOptional = findActiveById(componentOptionalId)
                .orElseThrow(() -> new RuntimeException("Component optional not found with id: " + componentOptionalId));
        
        componentOptional.setComponentOptionalName(componentOptionalDetails.getComponentOptionalName());
        componentOptional.setDescription(componentOptionalDetails.getDescription());
        
        // Update component if provided
        if (componentOptionalDetails.getComponent() != null && componentOptionalDetails.getComponent().getId() != null) {
            Component component = componentRepository.findActiveById(componentOptionalDetails.getComponent().getId())
                    .orElseThrow(() -> new RuntimeException("Component not found with id: " + componentOptionalDetails.getComponent().getId()));
            componentOptional.setComponent(component);
        }
        
        return componentOptionalRepository.save(componentOptional);
    }
    
    @Override
    protected void validateBeforeSave(ComponentOptional componentOptional) {
        // Validate component exists
        if (componentOptional.getComponent() == null || componentOptional.getComponent().getId() == null) {
            throw new IllegalArgumentException("Component is required");
        }
        
        Component component = componentRepository.findActiveById(componentOptional.getComponent().getId())
                .orElseThrow(() -> new RuntimeException("Component not found with id: " + componentOptional.getComponent().getId()));
        
        componentOptional.setComponent(component);
    }
    
    @Override
    public ComponentOptional save(ComponentOptional componentOptional) {
        validateBeforeSave(componentOptional);
        return super.save(componentOptional);
    }
}