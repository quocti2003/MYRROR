package com.mirror.product.service;

import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.repository.MirrorProductRepository;
import com.mirror.product.repository.ComponentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SkuService {

    private final MirrorProductRepository mirrorProductRepository;
    private final ComponentRepository componentRepository;

    public SkuService(MirrorProductRepository mirrorProductRepository, ComponentRepository componentRepository) {
        this.mirrorProductRepository = mirrorProductRepository;
        this.componentRepository = componentRepository;
    }

    public List<MirrorProduct> findAllActive() {
        return mirrorProductRepository.findAllActive();
    }

    public Optional<MirrorProduct> findActiveById(String id) {
        return mirrorProductRepository.findActiveById(id);
    }

    public Optional<MirrorProduct> findBySkuName(String skuName) {
        return mirrorProductRepository.findActiveBySkuName(skuName);
    }

    public Optional<MirrorProduct> findBySkuCode(String skuCode) {
        return mirrorProductRepository.findBySkuCode(skuCode);
    }

    public boolean existsBySkuName(String skuName) {
        return mirrorProductRepository.existsActiveBySkuName(skuName);
    }

    public long countActive() {
        return mirrorProductRepository.countActive();
    }

    public MirrorProduct update(String productId, MirrorProduct productDetails) {
        MirrorProduct product = findActiveById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        if (!product.getItemName().equals(productDetails.getItemName()) &&
            mirrorProductRepository.existsActiveBySkuNameAndNotId(productDetails.getItemName(), productId)) {
            throw new IllegalArgumentException("SKU name already exists: " + productDetails.getItemName());
        }

        product.setItemName(productDetails.getItemName());
        product.setDescription(productDetails.getDescription());

        if (productDetails.getSkuCode() != null) {
            String newCode = productDetails.getSkuCode();
            if (mirrorProductRepository.existsBySkuCodeAndNotId(newCode, productId)) {
                throw new IllegalArgumentException("SKU code already exists: " + newCode);
            }
            product.setSkuCode(newCode);
        }

        return mirrorProductRepository.save(product);
    }

    public MirrorProduct save(MirrorProduct product) {
        if (product.getId() == null || product.getId().isEmpty()) {
            validateBeforeSave(product);
        } else {
            MirrorProduct existing = findActiveById(product.getId()).orElse(null);
            if (existing != null) {
                if (!existing.getItemName().equals(product.getItemName()) &&
                        mirrorProductRepository.existsActiveBySkuNameAndNotId(product.getItemName(), product.getId())) {
                    throw new IllegalArgumentException("SKU name already exists: " + product.getItemName());
                }

                if (product.getSkuCode() != null && !product.getSkuCode().equals(existing.getSkuCode()) &&
                        mirrorProductRepository.existsBySkuCodeAndNotId(product.getSkuCode(), product.getId())) {
                    throw new IllegalArgumentException("SKU code already exists: " + product.getSkuCode());
                }
            }
        }
        return mirrorProductRepository.save(product);
    }

    public void softDeleteById(String productId) {
        validateBeforeDelete(productId);
        MirrorProduct product = mirrorProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        product.setIsDeleted(true);
        mirrorProductRepository.save(product);
    }

    public void deactivateById(String productId) {
        MirrorProduct product = mirrorProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        product.setIsActive(false);
        mirrorProductRepository.save(product);
    }

    private void validateBeforeSave(MirrorProduct product) {
        if (mirrorProductRepository.existsActiveBySkuName(product.getItemName())) {
            throw new IllegalArgumentException("SKU name already exists: " + product.getItemName());
        }
        if (product.getSkuCode() != null && mirrorProductRepository.existsBySkuCode(product.getSkuCode())) {
            throw new IllegalArgumentException("SKU code already exists: " + product.getSkuCode());
        }
    }

    private void validateBeforeDelete(String productId) {
        long componentCount = componentRepository.countActiveByProductId(productId);
        if (componentCount > 0) {
            throw new IllegalStateException("Cannot delete product. There are " + componentCount + " active components associated with this product.");
        }
    }
}
