package com.mirror.product.service;

import com.mirror.product.entity.ItemVariantConfig;
import com.mirror.product.entity.ItemVariant;
import com.mirror.product.entity.ComponentOptional;
import com.mirror.product.repository.ItemVariantConfigRepository;
import com.mirror.product.repository.ItemVariantRepository;
import com.mirror.product.repository.ComponentOptionalRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ItemVariantConfigService extends BaseService<ItemVariantConfig, String> {
    
    private final ItemVariantConfigRepository itemVariantConfigRepository;
    private final ItemVariantRepository itemVariantRepository;
    private final ComponentOptionalRepository componentOptionalRepository;
    
    public ItemVariantConfigService(
            ItemVariantConfigRepository itemVariantConfigRepository,
            ItemVariantRepository itemVariantRepository,
            ComponentOptionalRepository componentOptionalRepository) {
        super(itemVariantConfigRepository);
        this.itemVariantConfigRepository = itemVariantConfigRepository;
        this.itemVariantRepository = itemVariantRepository;
        this.componentOptionalRepository = componentOptionalRepository;
    }
    
    public List<ItemVariantConfig> findByItemVariantId(String itemVariantId) {
        return itemVariantConfigRepository.findActiveByItemVariantId(itemVariantId);
    }
    
    public List<ItemVariantConfig> findByComponentOptionalId(String componentOptionalId) {
        return itemVariantConfigRepository.findActiveByComponentOptionalId(componentOptionalId);
    }
    
    public Optional<ItemVariantConfig> findByItemVariantIdAndComponentOptionalId(String itemVariantId, String componentOptionalId) {
        return itemVariantConfigRepository.findActiveByItemVariantIdAndComponentOptionalId(itemVariantId, componentOptionalId);
    }
    
    public boolean existsByItemVariantIdAndComponentOptionalId(String itemVariantId, String componentOptionalId) {
        return itemVariantConfigRepository.existsActiveByItemVariantIdAndComponentOptionalId(itemVariantId, componentOptionalId);
    }
    
    @Override
    public ItemVariantConfig update(String itemVariantConfigId, ItemVariantConfig configDetails) {
        ItemVariantConfig config = findActiveById(itemVariantConfigId)
                .orElseThrow(() -> new RuntimeException("Item variant config not found with id: " + itemVariantConfigId));
        
        // Validate unique combination for update if changed
        String newVariantId = configDetails.getItemVariant().getId();
        String newComponentOptionalId = configDetails.getComponentOptional().getId();
        String currentVariantId = config.getItemVariant().getId();
        String currentComponentOptionalId = config.getComponentOptional().getId();
        
        if ((!currentVariantId.equals(newVariantId) || !currentComponentOptionalId.equals(newComponentOptionalId)) &&
            itemVariantConfigRepository.existsActiveByItemVariantIdAndComponentOptionalIdAndNotId(
                newVariantId, newComponentOptionalId, itemVariantConfigId)) {
            throw new IllegalArgumentException("Configuration already exists for this item variant and component optional combination");
        }
        
        // Validate foreign key references
        validateForeignKeyReferences(configDetails);
        
        config.setItemVariant(configDetails.getItemVariant());
        config.setComponentOptional(configDetails.getComponentOptional());
        
        return super.save(config);
    }
    
    @Override
    protected void validateBeforeSave(ItemVariantConfig config) {
        // Validate foreign key references
        validateForeignKeyReferences(config);
        
        // Validate unique combination
        if (itemVariantConfigRepository.existsActiveByItemVariantIdAndComponentOptionalId(
            config.getItemVariant().getId(),
            config.getComponentOptional().getId())) {
            throw new IllegalArgumentException("Configuration already exists for this item variant and component optional combination");
        }
    }
    
    private void validateForeignKeyReferences(ItemVariantConfig config) {
        // Validate item variant exists and is active
        if (config.getItemVariant() == null || config.getItemVariant().getId() == null) {
            throw new IllegalArgumentException("Item variant is required");
        }
        
        ItemVariant itemVariant = itemVariantRepository.findActiveById(config.getItemVariant().getId())
                .orElseThrow(() -> new RuntimeException("Item variant not found with id: " + config.getItemVariant().getId()));
        
        config.setItemVariant(itemVariant);
        
        // Validate component optional exists and is active
        if (config.getComponentOptional() == null || config.getComponentOptional().getId() == null) {
            throw new IllegalArgumentException("Component optional is required");
        }
        
        ComponentOptional componentOptional = componentOptionalRepository.findActiveById(config.getComponentOptional().getId())
                .orElseThrow(() -> new RuntimeException("Component optional not found with id: " + config.getComponentOptional().getId()));
        
        config.setComponentOptional(componentOptional);
    }
    
    @Override
    public ItemVariantConfig save(ItemVariantConfig config) {
        validateBeforeSave(config);
        return super.save(config);
    }
}