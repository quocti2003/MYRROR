package com.mirror.product.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.dto.ProductRequest;
import com.mirror.product.dto.ProductResponse;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.VendorProduct;
import com.mirror.product.enums.ProductStatus;
import com.mirror.product.repository.VendorProductRepository;
import com.mirror.product.repository.misa.MisaProductCategoryRepository;
import com.mirror.product.repository.ProductCertificateRepository;
import com.mirror.product.entity.misa.MisaProductCategory;
import com.mirror.product.entity.ProductCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class ProductMapper {

    private static final Logger log = LoggerFactory.getLogger(ProductMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory cache for categories (rarely change) - TTL 5 minutes
    private volatile Map<String, MisaProductCategory> categoryCache = null;
    private volatile long categoryCacheTime = 0;
    private static final long CATEGORY_CACHE_TTL_MS = 300000;

    @Autowired
    private VendorProductRepository vendorProductRepository;

    @Autowired
    private MisaProductCategoryRepository categoryRepository;

    @Autowired
    private ProductCertificateRepository productCertificateRepository;

    /**
     * Pre-load categories at application startup to avoid DB query during requests
     */
    @PostConstruct
    public void initCategoryCache() {
        loadCategoriesBatch();
    }

    /**
     * Convert ProductRequest to MirrorProduct entity for creation
     */
    public MirrorProduct toEntity(ProductRequest request) {
        if (request == null) {
            return null;
        }

        MirrorProduct product = new MirrorProduct();
        product.setItemName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setSkuCode(request.getSkuCode());
        product.setPrice(request.getPrice());
        product.setCurrency(request.getCurrency() != null ? request.getCurrency() : "VND");
        product.setMetalType(request.getMetalType());
        product.setMetalPurity(request.getMetalPurity());
        product.setStoneType(request.getStoneType());
        product.setWeightGrams(request.getWeightGrams());
        product.setDimensions(request.getDimensions());
        product.setImageUrl(request.getImageUrl());
        product.setStatus(request.getStatus() != null ? request.getStatus() : ProductStatus.DRAFT);
        product.setFeatured(request.getFeatured() != null ? request.getFeatured() : false);
        product.setModel3dId(request.getModel3dId());
        product.setModel3dNotRequired(request.getModel3dNotRequired() != null ? request.getModel3dNotRequired() : false);
        product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        product.setMinStockLevel(request.getMinStockLevel() != null ? request.getMinStockLevel() : 1);
        product.setCertificatesNotRequired(request.getCertificatesNotRequired() != null ? request.getCertificatesNotRequired() : false);
        // certificateCodes are handled separately via ProductService.handleCertificatesAssignment()

        // Convert lists to JSON strings
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            product.setImageUrls(convertListToJson(request.getImageUrls()));
        }
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            product.setTags(convertListToJson(request.getTags()));
        }

        return product;
    }

    /**
     * Update existing MirrorProduct entity with ProductRequest data
     */
    public void updateEntity(MirrorProduct existing, ProductRequest request) {
        if (existing == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            existing.setItemName(request.getName());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            existing.setCategory(request.getCategory());
        }
        if (request.getSkuCode() != null) {
            existing.setSkuCode(request.getSkuCode());
        }
        if (request.getPrice() != null) {
            existing.setPrice(request.getPrice());
        }
        if (request.getCurrency() != null) {
            existing.setCurrency(request.getCurrency());
        }
        if (request.getMetalType() != null) {
            existing.setMetalType(request.getMetalType());
        }
        if (request.getMetalPurity() != null) {
            existing.setMetalPurity(request.getMetalPurity());
        }
        if (request.getStoneType() != null) {
            existing.setStoneType(request.getStoneType());
        }
        if (request.getWeightGrams() != null) {
            existing.setWeightGrams(request.getWeightGrams());
        }
        if (request.getDimensions() != null) {
            existing.setDimensions(request.getDimensions());
        }
        if (request.getImageUrl() != null) {
            existing.setImageUrl(request.getImageUrl());
        }

        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (request.getFeatured() != null) {
            existing.setFeatured(request.getFeatured());
        }
        if (request.getModel3dId() != null) {
            existing.setModel3dId(request.getModel3dId());
        }
        if (request.getModel3dNotRequired() != null) {
            existing.setModel3dNotRequired(request.getModel3dNotRequired());
        }
        if (request.getStockQuantity() != null) {
            existing.setStockQuantity(request.getStockQuantity());
        }
        if (request.getMinStockLevel() != null) {
            existing.setMinStockLevel(request.getMinStockLevel());
        }
        if (request.getCertificatesNotRequired() != null) {
            existing.setCertificatesNotRequired(request.getCertificatesNotRequired());
        }
        // certificateCodes are handled separately via ProductService.handleCertificatesAssignment()

        // Update JSON fields
        if (request.getImageUrls() != null) {
            if (request.getImageUrls().isEmpty()) {
                existing.setImageUrls(null);
            } else {
                existing.setImageUrls(convertListToJson(request.getImageUrls()));
            }
        }
        if (request.getTags() != null) {
            if (request.getTags().isEmpty()) {
                existing.setTags(null);
            } else {
                existing.setTags(convertListToJson(request.getTags()));
            }
        }
    }

    /**
     * Convert MirrorProduct entity to ProductResponse
     */
    public ProductResponse toResponse(MirrorProduct product) {
        if (product == null) {
            return null;
        }

        ProductResponse response = new ProductResponse(product);

        // Parse JSON fields
        if (product.getDimensions() != null) {
            response.setDimensions(parseJsonObject(product.getDimensions()));
        }
        if (product.getImageUrls() != null) {
            response.setImageUrls(parseJsonStringList(product.getImageUrls()));
        }
        if (product.getTags() != null) {
            response.setTags(parseJsonStringList(product.getTags()));
        }

        // Load vendor information if available
        loadVendorInfo(product, response);

        // Load category name
        loadCategoryName(product, response);

        // Load certificate codes from join table
        loadCertificateCodes(product, response);

        return response;
    }

    /**
     * Convert list of MirrorProduct entities to list of ProductResponse
     * FAST version - only loads categories (cached at startup), skips vendor/certificate queries
     */
    public List<ProductResponse> toResponseList(List<MirrorProduct> products) {
        if (products == null || products.isEmpty()) {
            return products == null ? null : Collections.emptyList();
        }

        // Use category cache (loaded at startup)
        Map<String, MisaProductCategory> categoryMap = loadCategoriesBatch();

        // Map products using category cache only
        return products.stream()
                .map(product -> toResponseFast(product, categoryMap))
                .collect(Collectors.toList());
    }

    /**
     * Fast response mapping - only includes category name, skips vendor/certificate
     */
    private ProductResponse toResponseFast(MirrorProduct product, Map<String, MisaProductCategory> categoryMap) {
        if (product == null) {
            return null;
        }

        ProductResponse response = new ProductResponse(product);

        // Parse JSON fields
        if (product.getDimensions() != null) {
            response.setDimensions(parseJsonObject(product.getDimensions()));
        }
        if (product.getImageUrls() != null) {
            response.setImageUrls(parseJsonStringList(product.getImageUrls()));
        }
        if (product.getTags() != null) {
            response.setTags(parseJsonStringList(product.getTags()));
        }

        // Use cached category info
        loadCategoryNameFromCache(product, response, categoryMap);

        // Vendor and certificates are loaded on-demand for individual product views
        response.setCertificateCodes(Collections.emptyList());

        return response;
    }

    /**
     * Convert list of MirrorProduct entities to list of ProductResponse
     * FULL version with vendor and certificates - use for detailed views
     */
    public List<ProductResponse> toResponseListFull(List<MirrorProduct> products) {
        if (products == null || products.isEmpty()) {
            return products == null ? null : Collections.emptyList();
        }

        // Extract all product IDs for batch loading
        List<String> productIds = products.stream()
                .map(p -> String.valueOf(p.getId()))
                .collect(Collectors.toList());

        // Run all batch queries in PARALLEL to reduce latency
        CompletableFuture<Map<String, VendorProduct>> vendorFuture =
                CompletableFuture.supplyAsync(() -> loadVendorProductsBatch(productIds));
        CompletableFuture<Map<String, MisaProductCategory>> categoryFuture =
                CompletableFuture.supplyAsync(() -> loadCategoriesBatch());
        CompletableFuture<Map<String, List<String>>> certificateFuture =
                CompletableFuture.supplyAsync(() -> loadCertificateCodesBatch(productIds));

        // Wait for all to complete
        CompletableFuture.allOf(vendorFuture, categoryFuture, certificateFuture).join();

        Map<String, VendorProduct> vendorProductMap = vendorFuture.join();
        Map<String, MisaProductCategory> categoryMap = categoryFuture.join();
        Map<String, List<String>> certificateCodesMap = certificateFuture.join();

        // Map products using pre-loaded data
        return products.stream()
                .map(product -> toResponseWithBatchData(product, vendorProductMap, categoryMap, certificateCodesMap))
                .collect(Collectors.toList());
    }

    /**
     * Convert MirrorProduct to ProductResponse using pre-loaded batch data
     */
    private ProductResponse toResponseWithBatchData(
            MirrorProduct product,
            Map<String, VendorProduct> vendorProductMap,
            Map<String, MisaProductCategory> categoryMap,
            Map<String, List<String>> certificateCodesMap) {

        if (product == null) {
            return null;
        }

        ProductResponse response = new ProductResponse(product);

        // Parse JSON fields
        if (product.getDimensions() != null) {
            response.setDimensions(parseJsonObject(product.getDimensions()));
        }
        if (product.getImageUrls() != null) {
            response.setImageUrls(parseJsonStringList(product.getImageUrls()));
        }
        if (product.getTags() != null) {
            response.setTags(parseJsonStringList(product.getTags()));
        }

        // Use pre-loaded vendor info
        String productIdStr = String.valueOf(product.getId());
        VendorProduct vendorProduct = vendorProductMap.get(productIdStr);
        if (vendorProduct != null && vendorProduct.getVendor() != null) {
            response.setVendor(new ProductResponse.VendorResponse(vendorProduct.getVendor()));
        }

        // Use pre-loaded category info
        loadCategoryNameFromCache(product, response, categoryMap);

        // Use pre-loaded certificate codes
        List<String> certificateCodes = certificateCodesMap.getOrDefault(productIdStr, Collections.emptyList());
        response.setCertificateCodes(certificateCodes);

        return response;
    }

    /**
     * Batch load vendor products for multiple product IDs
     */
    private Map<String, VendorProduct> loadVendorProductsBatch(List<String> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<VendorProduct> vendorProducts = vendorProductRepository.findByProductIdIn(productIds);
            return vendorProducts.stream()
                    .collect(Collectors.toMap(
                            vp -> String.valueOf(vp.getProduct().getId()),
                            vp -> vp,
                            (existing, replacement) -> existing // Keep first if duplicates
                    ));
        } catch (Exception e) {
            log.error("Error batch loading vendor products: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Batch load all active categories into a map (with in-memory caching, 5 min TTL)
     */
    private Map<String, MisaProductCategory> loadCategoriesBatch() {
        // Check if cache is valid
        long now = System.currentTimeMillis();
        if (categoryCache != null && (now - categoryCacheTime) < CATEGORY_CACHE_TTL_MS) {
            return categoryCache;
        }

        try {
            List<MisaProductCategory> categories = categoryRepository.findByIsInactiveFalse();
            Map<String, MisaProductCategory> result = new java.util.HashMap<>();
            for (MisaProductCategory cat : categories) {
                // Map by ID
                result.put(String.valueOf(cat.getId()), cat);
                // Map by category code
                if (cat.getCategoryCode() != null) {
                    result.put(cat.getCategoryCode(), cat);
                }
            }
            // Update cache
            categoryCache = result;
            categoryCacheTime = now;
            return result;
        } catch (Exception e) {
            log.error("Error loading categories: {}", e.getMessage());
            return categoryCache != null ? categoryCache : Collections.emptyMap();
        }
    }

    /**
     * Batch load certificate codes for multiple product IDs
     */
    private Map<String, List<String>> loadCertificateCodesBatch(List<String> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<ProductCertificate> productCertificates = productCertificateRepository.findByProductIdIn(productIds);
            return productCertificates.stream()
                    .collect(Collectors.groupingBy(
                            pc -> String.valueOf(pc.getProduct().getId()),
                            Collectors.mapping(
                                    pc -> pc.getCertificate().getCertificateCode(),
                                    Collectors.toList()
                            )
                    ));
        } catch (Exception e) {
            log.error("Error batch loading certificates: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Load category name using pre-loaded category cache
     */
    private void loadCategoryNameFromCache(MirrorProduct product, ProductResponse response, Map<String, MisaProductCategory> categoryMap) {
        String categoryValue = product.getCategory();
        if (categoryValue == null || categoryValue.trim().isEmpty()) {
            return;
        }

        MisaProductCategory category = categoryMap.get(categoryValue);
        if (category != null) {
            response.setCategoryName(category.getCategoryName());
        } else {
            // Fallback: display the raw category value
            response.setCategoryName(categoryValue);
        }
    }

    /**
     * Convert MirrorProduct entity to simplified response for listings
     */
    public ProductResponse toSimpleResponse(MirrorProduct product) {
        if (product == null) {
            return null;
        }

        ProductResponse response = new ProductResponse();
        response.setId(String.valueOf(product.getId()));
        response.setName(product.getItemName());
        response.setSkuCode(product.getSkuCode());
        response.setPrice(product.getPrice());
        response.setCurrency(product.getCurrency());
        response.setImageUrl(product.getImageUrl());
        response.setStatus(product.getStatus());
        response.setFeatured(product.getFeatured());
        response.setStockQuantity(product.getStockQuantity());

        // Include basic tags for filtering
        if (product.getTags() != null) {
            response.setTags(parseJsonStringList(product.getTags()));
        }

        // Load vendor information if available
        loadVendorInfo(product, response);

        // Load category name
        loadCategoryName(product, response);

        return response;
    }

    /**
     * Convert list of MirrorProduct entities to simplified response list
     */
    public List<ProductResponse> toSimpleResponseList(List<MirrorProduct> products) {
        if (products == null) {
            return null;
        }

        return products.stream()
                .map(this::toSimpleResponse)
                .collect(Collectors.toList());
    }

    // Helper methods for JSON conversion
    private String convertListToJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> parseJsonStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private Object parseJsonObject(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    /**
     * Load vendor information for product if available
     */
    private void loadVendorInfo(MirrorProduct product, ProductResponse response) {
        try {
            // MirrorProduct uses Long ID, so convert to String for vendor lookup
            String productIdStr = String.valueOf(product.getId());
            java.util.Optional<VendorProduct> vendorProductOpt = vendorProductRepository.findByProductId(productIdStr);
            if (vendorProductOpt.isPresent()) {
                VendorProduct vendorProduct = vendorProductOpt.get();
                if (vendorProduct.getVendor() != null) {
                    response.setVendor(new ProductResponse.VendorResponse(vendorProduct.getVendor()));
                }
            }
        } catch (Exception e) {
            // Ignore vendor loading errors to prevent breaking product response
        }
    }

    /**
     * Load category name from misa_product_categories table
     */
    private void loadCategoryName(MirrorProduct product, ProductResponse response) {
        String categoryValue = product.getCategory();
        if (categoryValue == null || categoryValue.trim().isEmpty()) {
            return;
        }

        try {
            Optional<MisaProductCategory> categoryOpt = Optional.empty();

            // First, try to find by numeric ID
            try {
                Long categoryId = Long.parseLong(categoryValue);
                categoryOpt = categoryRepository.findById(categoryId);
            } catch (NumberFormatException e) {
                // Not a numeric ID, try by category_code
            }

            // If not found by ID, try by category_code
            if (categoryOpt.isEmpty()) {
                categoryOpt = categoryRepository.findByCategoryCode(categoryValue);
            }

            // Set the category name if found, otherwise use the raw category value as fallback
            if (categoryOpt.isPresent()) {
                response.setCategoryName(categoryOpt.get().getCategoryName());
            } else {
                // Fallback: display the raw category value (e.g., "RNG", "CNJ")
                response.setCategoryName(categoryValue);
            }
        } catch (Exception e) {
            // Fallback on error: display the raw category value
            response.setCategoryName(categoryValue);
        }
    }

    /**
     * Load certificate codes from product_certificates join table
     */
    private void loadCertificateCodes(MirrorProduct product, ProductResponse response) {
        try {
            List<String> certificateCodes = productCertificateRepository.findCertificateCodesByProductId(product.getId());
            response.setCertificateCodes(certificateCodes != null ? certificateCodes : Collections.emptyList());
        } catch (Exception e) {
            // Ignore errors and return empty list
            response.setCertificateCodes(Collections.emptyList());
        }
    }
}
