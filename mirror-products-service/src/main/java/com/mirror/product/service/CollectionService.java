package com.mirror.product.service;

import com.mirror.product.dto.CollectionRequest;
import com.mirror.product.dto.CollectionResponse;
import com.mirror.product.dto.CollectionProductRequest;
import com.mirror.product.dto.CollectionProductResponse;
import com.mirror.product.dto.ProductResponse;
import com.mirror.product.entity.Collection;
import com.mirror.product.entity.CollectionProduct;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.enums.ProductStatus;
import com.mirror.product.mapper.CollectionMapper;
import com.mirror.product.mapper.ProductMapper;
import com.mirror.product.repository.CollectionRepository;
import com.mirror.product.repository.CollectionProductRepository;
import com.mirror.product.repository.MirrorProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionService {
    
    private final CollectionRepository collectionRepository;
    private final CollectionMapper collectionMapper;
    private final CollectionProductRepository collectionProductRepository;
    private final MirrorProductRepository productRepository;
    private final ProductMapper productMapper;
    
    @Transactional(readOnly = true)
    public List<CollectionResponse> getAllActiveCollections() {
        List<Collection> collections = collectionRepository.findByStatusOrderBySortOrderAsc(Collection.CollectionStatus.ACTIVE);
        return collectionMapper.toResponseList(collections);
    }
    
    @Transactional(readOnly = true)
    public List<CollectionResponse> getAllCollections() {
        List<Collection> collections = collectionRepository.findAll();
        return collectionMapper.toResponseList(collections);
    }
    
    @Transactional(readOnly = true)
    public Optional<CollectionResponse> getCollectionById(String id) {
        return collectionRepository.findById(id)
                .map(collectionMapper::toResponse);
    }
    
    @Transactional
    public CollectionResponse createCollection(CollectionRequest request) {
        try {
            Collection collection = collectionMapper.toEntity(request);
            Collection savedCollection = collectionRepository.save(collection);
            log.info("Created new collection: {}", savedCollection.getId());
            return collectionMapper.toResponse(savedCollection);
        } catch (Exception e) {
            log.error("Error creating collection: {}", e.getMessage());
            throw new RuntimeException("Failed to create collection", e);
        }
    }
    
    @Transactional
    public Optional<CollectionResponse> updateCollection(String id, CollectionRequest request) {
        return collectionRepository.findById(id)
                .map(existingCollection -> {
                    try {
                        collectionMapper.updateEntity(existingCollection, request);
                        Collection updatedCollection = collectionRepository.save(existingCollection);
                        log.info("Updated collection: {}", updatedCollection.getId());
                        return collectionMapper.toResponse(updatedCollection);
                    } catch (Exception e) {
                        log.error("Error updating collection {}: {}", id, e.getMessage());
                        throw new RuntimeException("Failed to update collection", e);
                    }
                });
    }
    
    @Transactional
    public boolean deleteCollection(String id) {
        return collectionRepository.findById(id)
                .map(collection -> {
                    try {
                        collection.setStatus(Collection.CollectionStatus.ARCHIVED);
                        collectionRepository.save(collection);
                        log.info("Archived collection: {}", id);
                        return true;
                    } catch (Exception e) {
                        log.error("Error archiving collection {}: {}", id, e.getMessage());
                        return false;
                    }
                }).orElse(false);
    }
    
    @Transactional
    public boolean hardDeleteCollection(String id) {
        if (collectionRepository.existsById(id)) {
            try {
                collectionRepository.deleteById(id);
                log.info("Permanently deleted collection: {}", id);
                return true;
            } catch (Exception e) {
                log.error("Error permanently deleting collection {}: {}", id, e.getMessage());
                return false;
            }
        }
        return false;
    }

    // Collection-Product management methods
    @Transactional
    public boolean addProductToCollection(CollectionProductRequest request) {
        // Check if collection exists
        if (!collectionRepository.existsById(request.getCollectionId())) {
            log.error("Collection not found: {}", request.getCollectionId());
            return false;
        }
        
        // Check if product exists
        if (!productRepository.existsById(request.getProductId())) {
            log.error("Product not found: {}", request.getProductId());
            return false;
        }
        
        // Check if product is already in collection
        if (collectionProductRepository.existsByCollectionIdAndProductId(
                request.getCollectionId(), request.getProductId())) {
            log.warn("Product {} already in collection {}", request.getProductId(), request.getCollectionId());
            return false;
        }
        
        // Create new CollectionProduct
        CollectionProduct collectionProduct = CollectionProduct.builder()
                .collectionId(request.getCollectionId())
                .productId(request.getProductId())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isHeroProduct(request.getIsHeroProduct() != null ? request.getIsHeroProduct() : false)
                .build();
        
        collectionProductRepository.save(collectionProduct);
        log.info("Product {} added to collection {}", request.getProductId(), request.getCollectionId());
        return true;
    }
    
    @Transactional
    public boolean removeProductFromCollection(String collectionId, String productId) {
        Optional<CollectionProduct> collectionProduct = collectionProductRepository
                .findByCollectionIdAndProductId(collectionId, productId);
        
        if (collectionProduct.isPresent()) {
            collectionProductRepository.delete(collectionProduct.get());
            log.info("Product {} removed from collection {}", productId, collectionId);
            return true;
        }
        
        log.warn("Product {} not found in collection {}", productId, collectionId);
        return false;
    }
    
    @Transactional
    public boolean updateProductSortOrder(String collectionId, String productId, Integer sortOrder) {
        Optional<CollectionProduct> collectionProduct = collectionProductRepository
                .findByCollectionIdAndProductId(collectionId, productId);
        
        if (collectionProduct.isPresent()) {
            CollectionProduct cp = collectionProduct.get();
            cp.setSortOrder(sortOrder);
            collectionProductRepository.save(cp);
            log.info("Updated sort order for product {} in collection {} to {}", productId, collectionId, sortOrder);
            return true;
        }
        
        log.warn("Product {} not found in collection {}", productId, collectionId);
        return false;
    }
    
    @Transactional
    public Boolean toggleHeroProduct(String collectionId, String productId) {
        Optional<CollectionProduct> collectionProduct = collectionProductRepository
                .findByCollectionIdAndProductId(collectionId, productId);
        
        if (collectionProduct.isPresent()) {
            CollectionProduct cp = collectionProduct.get();
            boolean newHeroStatus = !cp.getIsHeroProduct();
            cp.setIsHeroProduct(newHeroStatus);
            collectionProductRepository.save(cp);
            log.info("Toggled hero status for product {} in collection {} to {}", productId, collectionId, newHeroStatus);
            return newHeroStatus;
        }
        
        log.warn("Product {} not found in collection {}", productId, collectionId);
        return null;
    }

    // Additional methods needed by controller (placeholder implementations)
    @Transactional(readOnly = true)
    public Optional<CollectionResponse> getCollectionByName(String name) {
        // TODO: Implement with repository method
        return Optional.empty();
    }
    
    @Transactional(readOnly = true)
    public Optional<CollectionResponse> getCollectionWithProducts(String id) {
        return collectionRepository.findById(id)
                .map(collection -> {
                    CollectionResponse response = collectionMapper.toResponse(collection);
                    
                    // Load products in this collection
                    List<CollectionProduct> collectionProducts = collectionProductRepository
                            .findByCollectionIdOrderBySortOrder(id);
                    
                    // Convert to product responses with collection details
                    List<CollectionProductResponse> productsWithDetails = collectionProducts.stream()
                            .map(cp -> {
                                Optional<MirrorProduct> productOpt = productRepository.findById(cp.getProductId());
                                if (productOpt.isPresent() && productOpt.get().getStatus() == ProductStatus.PUBLISHED) {
                                    ProductResponse productResponse = productMapper.toResponse(productOpt.get());
                                    return CollectionProductResponse.builder()
                                            .id(cp.getId())
                                            .collectionId(cp.getCollectionId())
                                            .productId(cp.getProductId())
                                            .sortOrder(cp.getSortOrder())
                                            .isHeroProduct(cp.getIsHeroProduct())
                                            .product(productResponse)
                                            .createdAt(cp.getCreatedAt())
                                            .updatedAt(cp.getUpdatedAt())
                                            .build();
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    
                    response.setProducts(productsWithDetails);
                    
                    // Keep featuredProducts for backward compatibility (hero products only)
                    List<ProductResponse> heroProducts = productsWithDetails.stream()
                            .filter(cp -> cp.getIsHeroProduct() != null && cp.getIsHeroProduct())
                            .map(CollectionProductResponse::getProduct)
                            .collect(Collectors.toList());
                    response.setFeaturedProducts(heroProducts);
                    return response;
                });
    }
    
    /**
     * Get all active collections with their products
     */
    @Transactional(readOnly = true)
    public List<CollectionResponse> getAllCollectionsWithProducts() {
        List<Collection> collections = collectionRepository.findByStatusOrderBySortOrderAsc(Collection.CollectionStatus.ACTIVE);
        
        return collections.stream()
                .map(collection -> {
                    CollectionResponse response = collectionMapper.toResponse(collection);
                    
                    // Load products for each collection
                    List<CollectionProduct> collectionProducts = collectionProductRepository
                            .findByCollectionIdOrderBySortOrder(collection.getId());
                    
                    List<ProductResponse> products = collectionProducts.stream()
                            .map(cp -> productRepository.findById(cp.getProductId()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(product -> product.getStatus() == ProductStatus.PUBLISHED)
                            .map(productMapper::toResponse)
                            .collect(Collectors.toList());

                    response.setFeaturedProducts(products);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get only products in collection (without collection info)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsInCollection(String collectionId) {
        List<CollectionProduct> collectionProducts = collectionProductRepository
                .findByCollectionIdOrderBySortOrder(collectionId);

        return collectionProducts.stream()
                .map(cp -> productRepository.findById(cp.getProductId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(product -> product.getStatus() == ProductStatus.PUBLISHED)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<CollectionResponse> getFeaturedCollections() {
        // TODO: Implement with repository method
        return List.of();
    }
    
    @Transactional(readOnly = true)
    public List<CollectionResponse> getCollectionsByYear(Integer year) {
        // TODO: Implement with repository method
        return List.of();
    }
    
    @Transactional(readOnly = true)
    public List<CollectionResponse> getCollectionsBySeasonAndYear(String season, Integer year) {
        // TODO: Implement with repository method
        return List.of();
    }
    
    @Transactional(readOnly = true)
    public List<CollectionResponse> searchCollections(String searchTerm) {
        // TODO: Implement with repository method
        return List.of();
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getFilterOptions() {
        // TODO: Implement with actual filter data
        return new HashMap<>();
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getCollectionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActiveCollections", collectionRepository.count());
        return stats;
    }
}