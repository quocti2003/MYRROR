package com.mirror.product.service;

import com.mirror.product.dto.DesignProductResponse;
import com.mirror.product.entity.DesignProduct;
import com.mirror.product.repository.DesignProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DesignProductService extends BaseService<DesignProduct, String> {

    private final DesignProductRepository designProductRepository;

    public DesignProductService(DesignProductRepository designProductRepository) {
        super(designProductRepository);
        this.designProductRepository = designProductRepository;
    }

    public List<DesignProductResponse> getDesignsByDesignerId(String designerId) {
        List<DesignProduct> designProducts = designProductRepository.findActiveByDesignerId(designerId);
        return designProducts.stream()
                .map(DesignProductResponse::new)
                .collect(Collectors.toList());
    }

    public Optional<DesignProduct> findByProductId(String productId) {
        return designProductRepository.findActiveByProductId(productId);
    }

    public List<DesignProduct> findByDesignerIdAndStatus(String designerId, String status) {
        return designProductRepository.findActiveByDesignerIdAndStatus(designerId, status);
    }

    public List<DesignProduct> findFeaturedDesigns() {
        return designProductRepository.findActiveFeaturedDesigns();
    }

    public List<DesignProduct> findBestSellingByDesignerId(String designerId) {
        return designProductRepository.findBestSellingDesignsByDesignerId(designerId);
    }

    @Override
    protected void validateBeforeSave(DesignProduct designProduct) {
        // Validate that product is not already assigned to another design
        if (designProduct.getId() == null || designProduct.getId().isEmpty()) {
            if (designProduct.getProduct() != null) {
                Optional<DesignProduct> existing = designProductRepository.findActiveByProductId(designProduct.getProduct().getId());
                if (existing.isPresent()) {
                    throw new IllegalArgumentException("Product is already assigned to another design: " + designProduct.getProduct().getId());
                }
            }
        }

        // Validate commission percentages
        if (designProduct.getCommissionPercentage() != null && designProduct.getLoyaltyPercentage() != null) {
            if (!designProduct.isValidPercentage()) {
                throw new IllegalArgumentException("Total commission and loyalty percentage cannot exceed 100%");
            }
        }
    }

    @Override
    public DesignProduct update(String designProductId, DesignProduct designProductDetails) {
        DesignProduct designProduct = findActiveById(designProductId)
                .orElseThrow(() -> new RuntimeException("Design product not found with id: " + designProductId));

        // Validate that product is not already assigned to another design (if changing product)
        if (designProductDetails.getProduct() != null &&
            !designProduct.getProduct().getId().equals(designProductDetails.getProduct().getId())) {
            Optional<DesignProduct> existing = designProductRepository.findActiveByProductId(designProductDetails.getProduct().getId());
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Product is already assigned to another design: " + designProductDetails.getProduct().getId());
            }
        }

        // Update fields
        if (designProductDetails.getCommissionPercentage() != null) {
            designProduct.setCommissionPercentage(designProductDetails.getCommissionPercentage());
        }
        if (designProductDetails.getLoyaltyPercentage() != null) {
            designProduct.setLoyaltyPercentage(designProductDetails.getLoyaltyPercentage());
        }
        if (designProductDetails.getDesignName() != null) {
            designProduct.setDesignName(designProductDetails.getDesignName());
        }
        if (designProductDetails.getDesignDescription() != null) {
            designProduct.setDesignDescription(designProductDetails.getDesignDescription());
        }
        if (designProductDetails.getDesignConcept() != null) {
            designProduct.setDesignConcept(designProductDetails.getDesignConcept());
        }
        if (designProductDetails.getDesignInspiration() != null) {
            designProduct.setDesignInspiration(designProductDetails.getDesignInspiration());
        }
        if (designProductDetails.getDesignStatus() != null) {
            designProduct.setDesignStatus(designProductDetails.getDesignStatus());
        }
        if (designProductDetails.getFeaturedDesign() != null) {
            designProduct.setFeaturedDesign(designProductDetails.getFeaturedDesign());
        }

        // Validate commission percentages after update
        if (!designProduct.isValidPercentage()) {
            throw new IllegalArgumentException("Total commission and loyalty percentage cannot exceed 100%");
        }

        return save(designProduct);
    }

    @Override
    protected void validateBeforeDelete(String designProductId) {
        // Add any validation logic before deleting a design product
        // For example, check if there are any sales transactions
    }
}