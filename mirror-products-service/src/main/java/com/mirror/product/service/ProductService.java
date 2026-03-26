package com.mirror.product.service;

import com.mirror.product.dto.ProductRequest;
import com.mirror.product.dto.ProductResponse;
import com.mirror.product.dto.ProductFulfillmentRequest;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.CollectionProduct;
import com.mirror.product.entity.Collection;
import com.mirror.product.entity.Vendor;
import com.mirror.product.entity.VendorProduct;
import com.mirror.product.enums.ProductStatus;
import com.mirror.product.mapper.ProductMapper;
import com.mirror.product.repository.MirrorProductRepository;
import com.mirror.product.repository.CollectionProductRepository;
import com.mirror.product.repository.CollectionRepository;
import com.mirror.product.repository.VendorRepository;
import com.mirror.product.repository.VendorProductRepository;
import com.mirror.product.repository.CertificateRepository;
import com.mirror.product.repository.ProductCertificateRepository;
import com.mirror.product.entity.Certificate;
import com.mirror.product.entity.ProductCertificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    @Autowired
    private MirrorProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private CollectionProductRepository collectionProductRepository;
    
    @Autowired
    private CollectionRepository collectionRepository;
    
    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorProductRepository vendorProductRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private ProductCertificateRepository productCertificateRepository;

    /**
     * Get all active products
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActiveProducts() {
        List<MirrorProduct> products = productRepository.findAllActive();
        return productMapper.toResponseList(products);
    }

    /**
     * Get all active products with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllActiveProducts(Pageable pageable) {
        Page<MirrorProduct> products = productRepository.findAll(pageable);
        return products.map(productMapper::toResponse);
    }

    /**
     * Get all products (including inactive) - Admin only
     * Note: No explicit @Transactional to avoid ~800ms BEGIN/COMMIT overhead with remote AWS RDS
     */
    public List<ProductResponse> getAllProducts() {
        List<MirrorProduct> products = productRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return productMapper.toResponseList(products);
    }

    /**
     * Get products by user's vendor
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByUserVendor(String userId) {
        // Find vendor for this user
        List<Vendor> vendors = vendorRepository.findActiveByOwnerUserId(userId);
        if (vendors.isEmpty()) {
            return List.of(); // Return empty list if no vendor
        }
        
        Vendor vendor = vendors.get(0); // Take first vendor
        return getProductsByVendorId(vendor.getId());
    }

    /**
     * Get products by vendor ID
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByVendorId(String vendorId) {
        // Find all VendorProduct relationships for this vendor
        List<VendorProduct> vendorProducts = vendorProductRepository.findByVendorId(vendorId);

        // Extract products from VendorProduct relationships
        List<MirrorProduct> products = vendorProducts.stream()
                .map(VendorProduct::getProduct)
                .filter(product -> product.getStatus() == ProductStatus.PUBLISHED) // Only active products
                .toList();

        return productMapper.toResponseList(products);
    }

    /**
     * Get product by ID
     */
    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProductById(String id) {
        return productRepository.findById(id)
                .map(productMapper::toResponse);
    }

    /**
     * Get product entity by ID (for internal use)
     */
    @Transactional(readOnly = true)
    public Optional<MirrorProduct> getProductEntityById(String id) {
        return productRepository.findById(id);
    }

    /**
     * Get product by SKU Code
     */
    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProductBySku(String skuCode) {
        return productRepository.findBySkuCode(skuCode)
                .map(productMapper::toResponse);
    }

    /**
     * Create a new product
     */
    public ProductResponse createProduct(ProductRequest request) {
        // Check for duplicate SKU code
        if (request.getSkuCode() != null && productRepository.findBySkuCode(request.getSkuCode()).isPresent()) {
            throw new IllegalArgumentException("Product with this SKU code already exists");
        }

        // Validate certificate codes list if provided
        validateCertificateCodes(request.getCertificateCodes());

        // Create and save product
        MirrorProduct product = productMapper.toEntity(request);
        MirrorProduct savedProduct = productRepository.save(product);

        // Handle vendor assignment if provided
        if (request.getVendorId() != null && !request.getVendorId().trim().isEmpty()) {
            handleVendorAssignment(savedProduct, request.getVendorId());
        }

        // Handle certificate codes assignment if provided
        handleCertificatesAssignment(savedProduct, request.getCertificateCodes());

        return productMapper.toResponse(savedProduct);
    }

    /**
     * Update an existing product
     */
    public Optional<ProductResponse> updateProduct(String id, ProductRequest request) {
        // Validate certificate codes list if provided
        validateCertificateCodes(request.getCertificateCodes());

        return productRepository.findById(id)
                .map(existingProduct -> {
                    // Check for duplicate SKU code if it's being changed
                    if (request.getSkuCode() != null &&
                        !request.getSkuCode().equals(existingProduct.getSkuCode())) {
                        Optional<MirrorProduct> existingWithSku = productRepository.findBySkuCode(request.getSkuCode());
                        if (existingWithSku.isPresent() && !existingWithSku.get().getId().equals(id)) {
                            throw new IllegalArgumentException("Product with this SKU code already exists");
                        }
                    }

                    productMapper.updateEntity(existingProduct, request);
                    MirrorProduct updatedProduct = productRepository.save(existingProduct);

                    // Handle vendor assignment if provided
                    if (request.getVendorId() != null && !request.getVendorId().trim().isEmpty()) {
                        handleVendorAssignment(updatedProduct, request.getVendorId());
                    } else {
                        // Remove existing vendor assignment if vendorId is null/empty
                        removeVendorAssignment(updatedProduct);
                    }

                    // Handle certificate codes assignment if provided (replaces existing)
                    if (request.getCertificateCodes() != null) {
                        handleCertificatesAssignment(updatedProduct, request.getCertificateCodes());
                    }

                    return productMapper.toResponse(updatedProduct);
                });
    }

    /**
     * Delete a product (soft delete)
     */
    public boolean deleteProduct(String id) {
        return productRepository.findById(id)
                .map(product -> {
                    // Remove from all collections first
                    removeProductFromAllCollections(id);
                    // Remove vendor assignment
                    removeVendorAssignment(product);
                    // Then mark as discontinued
                    product.setStatus(ProductStatus.ARCHIVED);
                    productRepository.save(product);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Hard delete a product
     */
    public boolean hardDeleteProduct(String id) {
        if (productRepository.existsById(id)) {
            MirrorProduct product = productRepository.findById(id).get();
            // Remove from all collections first
            removeProductFromAllCollections(id);
            // Remove vendor assignment
            removeVendorAssignment(product);
            // Then delete the product
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Get featured products
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts() {
        List<MirrorProduct> allProducts = productRepository.findAll();
        List<MirrorProduct> featuredProducts = allProducts.stream()
                .filter(p -> Boolean.TRUE.equals(p.getFeatured()) && p.getStatus() == ProductStatus.PUBLISHED)
                .toList();
        return productMapper.toResponseList(featuredProducts);
    }

    /**
     * Get products by category
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsBySku(String category) {
        List<MirrorProduct> products = productRepository.findByCategory(category);
        return productMapper.toResponseList(products);
    }

    /**
     * Get products by category with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsBySku(String category, Pageable pageable) {
        List<MirrorProduct> products = productRepository.findByCategory(category);
        return new org.springframework.data.domain.PageImpl<>(
                productMapper.toResponseList(products),
                pageable,
                products.size()
        );
    }

    /**
     * Get products by metal type
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByMetalType(String metalType) {
        List<MirrorProduct> allProducts = productRepository.findAll();
        List<MirrorProduct> filteredProducts = allProducts.stream()
                .filter(p -> metalType.equals(p.getMetalType()) && p.getStatus() == ProductStatus.PUBLISHED)
                .toList();
        return productMapper.toResponseList(filteredProducts);
    }

    /**
     * Get products by price range
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        List<MirrorProduct> allProducts = productRepository.findAll();
        List<MirrorProduct> filteredProducts = allProducts.stream()
                .filter(p -> p.getPrice() != null &&
                            p.getPrice().compareTo(minPrice) >= 0 &&
                            p.getPrice().compareTo(maxPrice) <= 0 &&
                            p.getStatus() == ProductStatus.PUBLISHED)
                .toList();
        return productMapper.toResponseList(filteredProducts);
    }

    /**
     * Search products by name
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProductsByName(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllActiveProducts();
        }

        List<MirrorProduct> allProducts = productRepository.findAll();
        String lowerSearchTerm = searchTerm.trim().toLowerCase();
        List<MirrorProduct> filteredProducts = allProducts.stream()
                .filter(p -> p.getItemName() != null &&
                            p.getItemName().toLowerCase().contains(lowerSearchTerm) &&
                            p.getStatus() == ProductStatus.PUBLISHED)
                .toList();
        return productMapper.toResponseList(filteredProducts);
    }

    /**
     * Advanced search products
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllActiveProducts(pageable);
        }

        List<MirrorProduct> allProducts = productRepository.findAll();
        String lowerSearchTerm = searchTerm.trim().toLowerCase();
        List<MirrorProduct> filteredProducts = allProducts.stream()
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED &&
                            ((p.getItemName() != null && p.getItemName().toLowerCase().contains(lowerSearchTerm)) ||
                             (p.getDescription() != null && p.getDescription().toLowerCase().contains(lowerSearchTerm)) ||
                             (p.getSkuCode() != null && p.getSkuCode().toLowerCase().contains(lowerSearchTerm)) ||
                             (p.getCategory() != null && p.getCategory().toLowerCase().contains(lowerSearchTerm)) ||
                             (p.getStoneType() != null && p.getStoneType().toLowerCase().contains(lowerSearchTerm))))
                .toList();
        return new org.springframework.data.domain.PageImpl<>(
                productMapper.toResponseList(filteredProducts),
                pageable,
                filteredProducts.size()
        );
    }

    /**
     * Get products with complex filtering
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsWithFilters(
            String category, String metalType, BigDecimal minPrice, BigDecimal maxPrice,
            Boolean featured, String sortBy, Pageable pageable) {

        List<MirrorProduct> allProducts = productRepository.findAll();
        List<MirrorProduct> filteredProducts = allProducts.stream()
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED)
                .filter(p -> category == null || category.equals(p.getCategory()))
                .filter(p -> metalType == null || metalType.equals(p.getMetalType()))
                .filter(p -> minPrice == null || (p.getPrice() != null && p.getPrice().compareTo(minPrice) >= 0))
                .filter(p -> maxPrice == null || (p.getPrice() != null && p.getPrice().compareTo(maxPrice) <= 0))
                .filter(p -> featured == null || featured.equals(p.getFeatured()))
                .toList();

        return new org.springframework.data.domain.PageImpl<>(
                productMapper.toResponseList(filteredProducts),
                pageable,
                filteredProducts.size()
        );
    }

    /**
     * Get available products (in stock)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAvailableProducts() {
        List<MirrorProduct> allProducts = productRepository.findAll();
        List<MirrorProduct> availableProducts = allProducts.stream()
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED &&
                            p.getStockQuantity() != null &&
                            p.getStockQuantity() > 0)
                .toList();
        return productMapper.toResponseList(availableProducts);
    }

    /**
     * Get available products with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAvailableProducts(Pageable pageable) {
        List<MirrorProduct> allProducts = productRepository.findAll();
        List<MirrorProduct> availableProducts = allProducts.stream()
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED &&
                            p.getStockQuantity() != null &&
                            p.getStockQuantity() > 0)
                .toList();
        return new org.springframework.data.domain.PageImpl<>(
                productMapper.toResponseList(availableProducts),
                pageable,
                availableProducts.size()
        );
    }

    /**
     * Get low stock products
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts() {
        List<MirrorProduct> products = productRepository.findLowStockProducts();
        return productMapper.toResponseList(products);
    }

    /**
     * Get out of stock products
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getOutOfStockProducts() {
        List<MirrorProduct> allProducts = productRepository.findAll();
        List<MirrorProduct> outOfStockProducts = allProducts.stream()
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED &&
                            (p.getStockQuantity() == null || p.getStockQuantity() == 0))
                .toList();
        return productMapper.toResponseList(outOfStockProducts);
    }

    /**
     * Update stock quantity
     */
    public boolean updateStock(String id, Integer quantity) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setStockQuantity(quantity);

                    // Auto-update status based on stock - commented out as OUT_OF_STOCK status removed
                    // if (quantity == 0 && product.getStatus() == ProductStatus.PUBLISHED) {
                    //     product.setStatus(ProductStatus.ARCHIVED);
                    // } else if (quantity > 0 && product.getStatus() == ProductStatus.ARCHIVED) {
                    //     product.setStatus(ProductStatus.PUBLISHED);
                    // }

                    productRepository.save(product);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Toggle featured status
     */
    public boolean toggleFeatured(String id) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setFeatured(!product.getFeatured());
                    productRepository.save(product);
                    return product.getFeatured();
                })
                .orElse(false);
    }

    /**
     * Get filter options
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFilterOptions() {
        Map<String, Object> filters = new HashMap<>();

        List<MirrorProduct> publishedProducts = productRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED)
                .toList();

        // Get distinct categories
        List<String> categories = publishedProducts.stream()
                .map(MirrorProduct::getCategory)
                .filter(c -> c != null)
                .distinct()
                .toList();
        filters.put("categories", categories);

        // Get distinct metal types
        List<String> metalTypes = publishedProducts.stream()
                .map(MirrorProduct::getMetalType)
                .filter(mt -> mt != null)
                .distinct()
                .toList();
        filters.put("metalTypes", metalTypes);

        // Get distinct stone types
        List<String> stoneTypes = publishedProducts.stream()
                .map(MirrorProduct::getStoneType)
                .filter(st -> st != null)
                .distinct()
                .toList();
        filters.put("stoneTypes", stoneTypes);

        // Get price range
        BigDecimal minPrice = publishedProducts.stream()
                .map(MirrorProduct::getPrice)
                .filter(p -> p != null)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = publishedProducts.stream()
                .map(MirrorProduct::getPrice)
                .filter(p -> p != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        Map<String, BigDecimal> priceRangeMap = new HashMap<>();
        priceRangeMap.put("min", minPrice);
        priceRangeMap.put("max", maxPrice);
        filters.put("priceRange", priceRangeMap);

        return filters;
    }

    /**
     * Get product statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProductStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<MirrorProduct> allProducts = productRepository.findAll();

        // Total active products
        long totalActive = allProducts.stream()
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED)
                .count();
        stats.put("totalActiveProducts", totalActive);

        // Featured products count
        long totalFeatured = allProducts.stream()
                .filter(p -> Boolean.TRUE.equals(p.getFeatured()) && p.getStatus() == ProductStatus.PUBLISHED)
                .count();
        stats.put("totalFeaturedProducts", totalFeatured);

        // Count by status
        Map<String, Long> byStatus = new HashMap<>();
        for (ProductStatus status : ProductStatus.values()) {
            long count = allProducts.stream()
                    .filter(p -> p.getStatus() == status)
                    .count();
            byStatus.put(status.name(), count);
        }
        stats.put("productsByStatus", byStatus);

        // Low stock count
        long lowStockCount = productRepository.findLowStockProducts().size();
        stats.put("lowStockProducts", lowStockCount);

        // Out of stock count
        long outOfStockCount = allProducts.stream()
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED &&
                            (p.getStockQuantity() == null || p.getStockQuantity() == 0))
                .count();
        stats.put("outOfStockProducts", outOfStockCount);

        return stats;
    }

    /**
     * Check if product exists
     */
    @Transactional(readOnly = true)
    public boolean productExists(String id) {
        return productRepository.existsById(id);
    }
    
    /**
     * Remove product from all collections when product is deleted
     */
    private void removeProductFromAllCollections(String productId) {
        List<CollectionProduct> collectionProducts = collectionProductRepository.findByProductId(productId);
        if (!collectionProducts.isEmpty()) {
            collectionProductRepository.deleteAll(collectionProducts);
        }
    }
    
    /**
     * Handle vendor assignment for product
     */
    private void handleVendorAssignment(MirrorProduct product, String vendorId) {
        // Validate vendor exists and is active
        Optional<Vendor> vendorOpt = vendorRepository.findActiveById(vendorId);
        if (vendorOpt.isEmpty()) {
            throw new IllegalArgumentException("Vendor not found or inactive: " + vendorId);
        }

        Vendor vendor = vendorOpt.get();

        // Check if product already has a vendor assignment
        String productIdStr = String.valueOf(product.getId());
        Optional<VendorProduct> existingVendorProduct = vendorProductRepository.findByProductId(productIdStr);

        if (existingVendorProduct.isPresent()) {
            // Update existing vendor assignment
            VendorProduct vendorProduct = existingVendorProduct.get();
            vendorProduct.setVendor(vendor);
            vendorProduct.setCommissionTerm(null); // Keep commission term as null as requested
            vendorProductRepository.save(vendorProduct);
        } else {
            // Create new vendor assignment
            VendorProduct vendorProduct = VendorProduct.builder()
                    .vendor(vendor)
                    .product(product)
                    .commissionTerm(null) // Keep commission term as null as requested
                    .isActive(true)
                    .build();
            vendorProductRepository.save(vendorProduct);
        }
    }

    /**
     * Remove vendor assignment for product (hard delete)
     */
    private void removeVendorAssignment(MirrorProduct product) {
        vendorProductRepository.deleteByProductId(String.valueOf(product.getId()));
    }

    /**
     * Validate that all certificate codes exist in certificates table
     * @param certificateCodes the list of certificate codes to validate (can be null or empty)
     * @throws IllegalArgumentException if any certificate code doesn't exist
     */
    private void validateCertificateCodes(List<String> certificateCodes) {
        if (certificateCodes == null || certificateCodes.isEmpty()) {
            return; // No certificate codes provided, skip validation
        }

        for (String code : certificateCodes) {
            if (code != null && !code.trim().isEmpty()) {
                boolean exists = certificateRepository.existsByCertificateCode(code.trim());
                if (!exists) {
                    throw new IllegalArgumentException("Certificate not found with code: " + code);
                }
            }
        }
    }

    /**
     * Handle certificates assignment for product (replaces existing)
     * @param product the product to assign certificates to
     * @param certificateCodes list of certificate codes to assign
     */
    private void handleCertificatesAssignment(MirrorProduct product, List<String> certificateCodes) {
        if (certificateCodes == null) {
            return; // null means don't change existing assignments
        }

        // Remove existing certificate assignments
        productCertificateRepository.deleteByProductId(product.getId());

        // Add new certificate assignments
        if (!certificateCodes.isEmpty()) {
            for (String code : certificateCodes) {
                if (code != null && !code.trim().isEmpty()) {
                    Certificate certificate = certificateRepository.findByCertificateCode(code.trim())
                            .orElseThrow(() -> new IllegalArgumentException("Certificate not found with code: " + code));

                    ProductCertificate productCertificate = new ProductCertificate(product, certificate);
                    productCertificateRepository.save(productCertificate);
                }
            }
        }
    }

    // ==================== NEW PRODUCTION FLOW METHODS ====================

    /**
     * Get all products in DRAFT status
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getDraftProducts() {
        List<MirrorProduct> allProducts = productRepository.findAll();
        List<MirrorProduct> draftProducts = allProducts.stream()
                .filter(p -> p.getStatus() == ProductStatus.DRAFT)
                .toList();
        return productMapper.toResponseList(draftProducts);
    }

    /**
     * Get products pending fulfillment (DRAFT with missing images, description, certificates, or 3D model)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsPendingFulfillment() {
        List<MirrorProduct> allProducts = productRepository.findAll();
        List<MirrorProduct> pendingProducts = allProducts.stream()
            .filter(p -> p.getStatus() == ProductStatus.DRAFT)
            .filter(p -> !hasValidImageUrls(p) || !hasValidDescription(p) || !hasValidCertificates(p) || !hasValidModel3d(p))
            .collect(Collectors.toList());
        return productMapper.toResponseList(pendingProducts);
    }

    /**
     * Get fulfilled drafts (DRAFT products with images AND description AND certificates AND 3D model, ready to be released)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getFulfilledDrafts() {
        List<MirrorProduct> allProducts = productRepository.findAll();
        List<MirrorProduct> fulfilledDrafts = allProducts.stream()
            .filter(p -> p.getStatus() == ProductStatus.DRAFT)
            .filter(p -> hasValidImageUrls(p))
            .filter(p -> hasValidDescription(p))
            .filter(p -> hasValidCertificates(p))
            .filter(p -> hasValidModel3d(p))
            .collect(Collectors.toList());
        return productMapper.toResponseList(fulfilledDrafts);
    }

    /**
     * Check if product has valid image URLs (not null, not empty JSON array)
     */
    private boolean hasValidImageUrls(MirrorProduct product) {
        String imageUrls = product.getImageUrls();
        if (imageUrls == null || imageUrls.trim().isEmpty()) {
            return false;
        }
        // Parse JSON array and check if it has actual URLs
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<String> urls = objectMapper.readValue(imageUrls, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            return urls != null && !urls.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if product has valid description
     */
    private boolean hasValidDescription(MirrorProduct product) {
        String description = product.getDescription();
        return description != null && !description.trim().isEmpty();
    }

    /**
     * Check if product has valid certificates (or certificates are not required)
     */
    private boolean hasValidCertificates(MirrorProduct product) {
        // If certificates are not required, consider this check as passed
        if (Boolean.TRUE.equals(product.getCertificatesNotRequired())) {
            return true;
        }
        // Otherwise, check if product has at least one certificate
        List<String> certCodes = productCertificateRepository.findCertificateCodesByProductId(product.getId());
        return certCodes != null && !certCodes.isEmpty();
    }

    /**
     * Check if product has valid 3D model (or 3D model is not required)
     */
    private boolean hasValidModel3d(MirrorProduct product) {
        // If 3D model is not required, consider this check as passed
        if (Boolean.TRUE.equals(product.getModel3dNotRequired())) {
            return true;
        }
        // Otherwise, check if product has a model3dId
        String model3dId = product.getModel3dId();
        return model3dId != null && !model3dId.trim().isEmpty();
    }

    /**
     * Fulfill product with images, assets, and description
     */
    public ProductResponse fulfillProduct(String productId, ProductFulfillmentRequest request) {
        MirrorProduct product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (product.getStatus() != ProductStatus.DRAFT && product.getStatus() != ProductStatus.READY_FOR_RELEASE) {
            throw new IllegalStateException("Only DRAFT or READY_FOR_RELEASE products can be fulfilled");
        }

        // Update product with fulfillment data
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            product.setImageUrls(String.join(",", request.getImageUrls()));
        }

        // Mark fulfillment as completed
        product.setFulfillmentCompletedAt(LocalDateTime.now());

        // Optionally mark as ready for release
        if (Boolean.TRUE.equals(request.getMarkReadyForRelease())) {
            product.setStatus(ProductStatus.READY_FOR_RELEASE);
        }

        MirrorProduct savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Mark product as ready for release
     */
    public ProductResponse markReadyForRelease(String productId) {
        MirrorProduct product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (product.getStatus() != ProductStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT products can be marked as ready for release");
        }

        // Validate that product has required data
        if (!hasValidDescription(product)) {
            throw new IllegalStateException("Product must have a description before being marked ready for release");
        }

        if (!hasValidImageUrls(product)) {
            throw new IllegalStateException("Product must have images before being marked ready for release");
        }

        if (!hasValidCertificates(product)) {
            throw new IllegalStateException("Product must have certificates (or be marked as 'certificates not required') before being marked ready for release");
        }

        if (!hasValidModel3d(product)) {
            throw new IllegalStateException("Product must have a 3D model (or be marked as '3D model not required') before being marked ready for release");
        }

        product.setStatus(ProductStatus.READY_FOR_RELEASE);
        product.setFulfillmentCompletedAt(LocalDateTime.now());

        MirrorProduct savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Publish product to website
     */
    public ProductResponse publishProduct(String productId) {
        MirrorProduct product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (product.getStatus() != ProductStatus.READY_FOR_RELEASE) {
            throw new IllegalStateException("Only READY_FOR_RELEASE products can be published");
        }

        product.setStatus(ProductStatus.PUBLISHED);
        product.setPublishedAt(LocalDateTime.now());

        MirrorProduct savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Unpublish product (move back to READY_FOR_RELEASE)
     */
    public ProductResponse unpublishProduct(String productId) {
        MirrorProduct product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED products can be unpublished");
        }

        product.setStatus(ProductStatus.READY_FOR_RELEASE);
        product.setPublishedAt(null);

        MirrorProduct savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Archive product
     */
    public ProductResponse archiveProduct(String productId) {
        MirrorProduct product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        product.setStatus(ProductStatus.ARCHIVED);

        MirrorProduct savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Get products by status
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByStatus(ProductStatus status) {
        List<MirrorProduct> allProducts = productRepository.findAll();
        List<MirrorProduct> filteredProducts = allProducts.stream()
                .filter(p -> p.getStatus() == status)
                .toList();
        return productMapper.toResponseList(filteredProducts);
    }

}
