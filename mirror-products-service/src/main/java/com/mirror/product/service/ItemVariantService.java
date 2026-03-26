package com.mirror.product.service;

import com.mirror.product.entity.ItemVariant;
import com.mirror.product.repository.ItemVariantRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ItemVariantService extends BaseService<ItemVariant, String> {
    
    private final ItemVariantRepository itemVariantRepository;
    
    public ItemVariantService(ItemVariantRepository itemVariantRepository) {
        super(itemVariantRepository);
        this.itemVariantRepository = itemVariantRepository;
    }
    
    public Optional<ItemVariant> findByItemVariantUrl(String itemVariantUrl) {
        return itemVariantRepository.findActiveByItemVariantUrl(itemVariantUrl);
    }
    
    public boolean existsByItemVariantUrl(String itemVariantUrl) {
        return itemVariantRepository.existsActiveByItemVariantUrl(itemVariantUrl);
    }
    
    @Override
    public ItemVariant update(String itemVariantId, ItemVariant itemVariantDetails) {
        ItemVariant itemVariant = findActiveById(itemVariantId)
                .orElseThrow(() -> new RuntimeException("Item variant not found with id: " + itemVariantId));
        
        // Validate unique item variant URL for update
        if (!itemVariant.getItemVariantUrl().equals(itemVariantDetails.getItemVariantUrl()) &&
            itemVariantRepository.existsActiveByItemVariantUrlAndNotId(itemVariantDetails.getItemVariantUrl(), itemVariantId)) {
            throw new IllegalArgumentException("Item variant URL already exists: " + itemVariantDetails.getItemVariantUrl());
        }
        
        itemVariant.setItemVariantUrl(itemVariantDetails.getItemVariantUrl());
        itemVariant.setDescription(itemVariantDetails.getDescription());
        
        return save(itemVariant);
    }
    
    @Override
    protected void validateBeforeSave(ItemVariant itemVariant) {
        if (itemVariantRepository.existsActiveByItemVariantUrl(itemVariant.getItemVariantUrl())) {
            throw new IllegalArgumentException("Item variant URL already exists: " + itemVariant.getItemVariantUrl());
        }
    }
    
    @Override
    public ItemVariant save(ItemVariant itemVariant) {
        validateBeforeSave(itemVariant);
        return super.save(itemVariant);
    }
}