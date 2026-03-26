package com.mirror.product.service;

import com.mirror.product.entity.Component;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.repository.ComponentRepository;
import com.mirror.product.repository.MirrorProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ComponentService extends BaseService<Component, String> {

    private final ComponentRepository componentRepository;
    private final MirrorProductRepository mirrorProductRepository;

    public ComponentService(ComponentRepository componentRepository, MirrorProductRepository mirrorProductRepository) {
        super(componentRepository);
        this.componentRepository = componentRepository;
        this.mirrorProductRepository = mirrorProductRepository;
    }

    public List<Component> findByProductId(String productId) {
        return componentRepository.findActiveByProductId(productId);
    }

    public Optional<Component> findByComponentName(String componentName) {
        return componentRepository.findActiveByComponentName(componentName);
    }

    public boolean existsByComponentNameAndProductId(String componentName, String productId) {
        return componentRepository.existsActiveByComponentNameAndProductId(componentName, productId);
    }

    @Override
    public Component update(String componentId, Component componentDetails) {
        Component component = findActiveById(componentId)
                .orElseThrow(() -> new RuntimeException("Component not found with id: " + componentId));

        component.setComponentName(componentDetails.getComponentName());
        component.setDescription(componentDetails.getDescription());

        if (componentDetails.getProduct() != null && componentDetails.getProduct().getId() != null) {
            MirrorProduct product = mirrorProductRepository.findById(componentDetails.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + componentDetails.getProduct().getId()));
            component.setProduct(product);
        }

        return super.save(component);
    }

    @Override
    protected void validateBeforeSave(Component component) {
        if (component.getProduct() == null || component.getProduct().getId() == null) {
            throw new IllegalArgumentException("Product is required");
        }

        MirrorProduct product = mirrorProductRepository.findById(component.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + component.getProduct().getId()));

        component.setProduct(product);
    }

    @Override
    public Component save(Component component) {
        validateBeforeSave(component);
        return super.save(component);
    }
}
