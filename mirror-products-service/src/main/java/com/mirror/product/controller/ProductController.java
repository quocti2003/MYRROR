package com.mirror.product.controller;

import com.mirror.product.dto.ProductRequest;
import com.mirror.product.dto.ProductResponse;
import com.mirror.product.dto.ProductFulfillmentRequest;
import com.mirror.product.enums.ProductStatus;
import com.mirror.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * Get all active products
     * GET /api/products
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        if (paginated) {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductResponse> products = productService.getAllActiveProducts(pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(products.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(products.getTotalPages()))
                    .body(products.getContent());
        } else {
            List<ProductResponse> products = productService.getAllActiveProducts();
            return ResponseEntity.ok(products);
        }
    }

    /**
     * Get all products including inactive (admin)
     * GET /api/products/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<ProductResponse>> getAllProductsIncludingInactive() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Get product by ID
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        return productService.getProductById(id)
                .map(product -> ResponseEntity.ok(product))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get product by SKU
     * GET /api/products/sku/{sku}
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku) {
        return productService.getProductBySku(sku)
                .map(product -> ResponseEntity.ok(product))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new product
     * POST /api/products
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        try {
            ProductResponse createdProduct = productService.createProduct(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing product
     * PUT /api/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        try {
            return productService.updateProduct(id, request)
                    .map(product -> ResponseEntity.ok(product))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a product (soft delete)
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable String id) {
        boolean deleted = productService.deleteProduct(id);
        
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                "message", "Product discontinued successfully",
                "id", id,
                "status", "DISCONTINUED"
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Hard delete a product (permanent removal)
     * DELETE /api/products/{id}/permanent
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Map<String, Object>> hardDeleteProduct(@PathVariable String id) {
        boolean deleted = productService.hardDeleteProduct(id);
        
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                "message", "Product permanently deleted",
                "id", id
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get featured products
     * GET /api/products/featured
     */
    @GetMapping("/featured")
    public ResponseEntity<List<ProductResponse>> getFeaturedProducts() {
        List<ProductResponse> products = productService.getFeaturedProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Get available products (in stock)
     * GET /api/products/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<ProductResponse>> getAvailableProducts(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        if (paginated) {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductResponse> products = productService.getAvailableProducts(pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(products.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(products.getTotalPages()))
                    .body(products.getContent());
        } else {
            List<ProductResponse> products = productService.getAvailableProducts();
            return ResponseEntity.ok(products);
        }
    }

    /**
     * Get products by SKU definition
     * GET /api/products/sku-definition/{skuId}
     */
    @GetMapping("/sku-definition/{skuId}")
    public ResponseEntity<List<ProductResponse>> getProductsBySku(
            @PathVariable String skuId,
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (paginated) {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductResponse> products = productService.getProductsBySku(skuId, pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(products.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(products.getTotalPages()))
                    .body(products.getContent());
        } else {
            List<ProductResponse> products = productService.getProductsBySku(skuId);
            return ResponseEntity.ok(products);
        }
    }

    // Legacy category-based route retained for compatibility
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategoryLegacy(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return getProductsBySku(categoryId, paginated, page, size);
    }

    /**
     * Get products by metal type
     * GET /api/products/metal/{metalType}
     */
    @GetMapping("/metal/{metalType}")
    public ResponseEntity<List<ProductResponse>> getProductsByMetalType(@PathVariable String metalType) {
        List<ProductResponse> products = productService.getProductsByMetalType(metalType);
        return ResponseEntity.ok(products);
    }

    /**
     * Search products
     * GET /api/products/search?q={searchTerm}
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @RequestParam("q") String searchTerm,
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        if (paginated) {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductResponse> products = productService.searchProducts(searchTerm, pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(products.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(products.getTotalPages()))
                    .body(products.getContent());
        } else {
            List<ProductResponse> products = productService.searchProductsByName(searchTerm);
            return ResponseEntity.ok(products);
        }
    }

    /**
     * Advanced product filtering
     * GET /api/products/filter
     */
    @GetMapping("/filter")
    public ResponseEntity<List<ProductResponse>> getProductsWithFilters(
            @RequestParam(required = false) String skuId,
            @RequestParam(required = false, name = "categoryId") String legacyCategoryId,
            @RequestParam(required = false) String metalType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "created_desc") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        if (skuId == null && legacyCategoryId != null) {
            skuId = legacyCategoryId;
        }

        Page<ProductResponse> products = productService.getProductsWithFilters(
                skuId, metalType, minPrice, maxPrice, featured, sortBy, pageable);
        
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(products.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(products.getTotalPages()))
                .body(products.getContent());
    }

    /**
     * Get products by price range
     * GET /api/products/price-range?min={minPrice}&max={maxPrice}
     */
    @GetMapping("/price-range")
    public ResponseEntity<List<ProductResponse>> getProductsByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        
        List<ProductResponse> products = productService.getProductsByPriceRange(min, max);
        return ResponseEntity.ok(products);
    }

    /**
     * Get low stock products
     * GET /api/products/low-stock
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponse>> getLowStockProducts() {
        List<ProductResponse> products = productService.getLowStockProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Get out of stock products
     * GET /api/products/out-of-stock
     */
    @GetMapping("/out-of-stock")
    public ResponseEntity<List<ProductResponse>> getOutOfStockProducts() {
        List<ProductResponse> products = productService.getOutOfStockProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Update stock quantity
     * PUT /api/products/{id}/stock
     */
    @PutMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> updateStock(
            @PathVariable String id,
            @RequestBody Map<String, Integer> request) {
        
        Integer quantity = request.get("quantity");
        if (quantity == null || quantity < 0) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean updated = productService.updateStock(id, quantity);
        
        if (updated) {
            return ResponseEntity.ok(Map.of(
                "message", "Stock updated successfully",
                "id", id,
                "newQuantity", quantity
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Toggle featured status
     * POST /api/products/{id}/toggle-featured
     */
    @PostMapping("/{id}/toggle-featured")
    public ResponseEntity<Map<String, Object>> toggleFeatured(@PathVariable String id) {
        boolean featured = productService.toggleFeatured(id);
        
        return ResponseEntity.ok(Map.of(
            "message", "Featured status updated",
            "id", id,
            "featured", featured
        ));
    }

    /**
     * Get filter options
     * GET /api/products/filters
     */
    @GetMapping("/filters")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        Map<String, Object> filters = productService.getFilterOptions();
        return ResponseEntity.ok(filters);
    }

    /**
     * Get product statistics
     * GET /api/products/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getProductStatistics() {
        Map<String, Object> statistics = productService.getProductStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Health check endpoint
     * GET /api/products/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        long totalProducts = (Long) productService.getProductStatistics()
                .getOrDefault("totalActiveProducts", 0L);

        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "service", "ProductService",
            "totalActiveProducts", totalProducts,
            "timestamp", System.currentTimeMillis()
        ));
    }

    // ==================== NEW PRODUCTION FLOW ENDPOINTS ====================

    /**
     * Get all DRAFT products
     * GET /api/products/draft
     */
    @GetMapping("/draft")
    public ResponseEntity<List<ProductResponse>> getDraftProducts() {
        List<ProductResponse> products = productService.getDraftProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Get products pending fulfillment (missing images or description)
     * GET /api/products/pending-fulfillment
     */
    @GetMapping("/pending-fulfillment")
    public ResponseEntity<List<ProductResponse>> getProductsPendingFulfillment() {
        List<ProductResponse> products = productService.getProductsPendingFulfillment();
        return ResponseEntity.ok(products);
    }

    /**
     * Get fulfilled drafts (DRAFT products ready to be released)
     * GET /api/products/fulfilled-drafts
     */
    @GetMapping("/fulfilled-drafts")
    public ResponseEntity<List<ProductResponse>> getFulfilledDrafts() {
        List<ProductResponse> products = productService.getFulfilledDrafts();
        return ResponseEntity.ok(products);
    }

    /**
     * Get products ready for release (status = READY_FOR_RELEASE)
     * GET /api/products/ready-for-release
     */
    @GetMapping("/ready-for-release")
    public ResponseEntity<List<ProductResponse>> getReadyForRelease() {
        List<ProductResponse> products = productService.getProductsByStatus(ProductStatus.READY_FOR_RELEASE);
        return ResponseEntity.ok(products);
    }

    /**
     * Get published products (status = PUBLISHED)
     * GET /api/products/published
     */
    @GetMapping("/published")
    public ResponseEntity<List<ProductResponse>> getPublishedProducts() {
        List<ProductResponse> products = productService.getProductsByStatus(ProductStatus.PUBLISHED);
        return ResponseEntity.ok(products);
    }

    /**
     * Fulfill product with images, assets, and description
     * PUT /api/products/{id}/fulfill
     */
    @PutMapping("/{id}/fulfill")
    public ResponseEntity<ProductResponse> fulfillProduct(
            @PathVariable String id,
            @RequestBody ProductFulfillmentRequest request) {
        request.setProductId(id);
        ProductResponse product = productService.fulfillProduct(id, request);
        return ResponseEntity.ok(product);
    }

    /**
     * Mark product as ready for release
     * PUT /api/products/{id}/ready-for-release
     */
    @PutMapping("/{id}/ready-for-release")
    public ResponseEntity<ProductResponse> markReadyForRelease(@PathVariable String id) {
        ProductResponse product = productService.markReadyForRelease(id);
        return ResponseEntity.ok(product);
    }

    /**
     * Publish product to website
     * PUT /api/products/{id}/publish
     */
    @PutMapping("/{id}/publish")
    public ResponseEntity<ProductResponse> publishProduct(@PathVariable String id) {
        ProductResponse product = productService.publishProduct(id);
        return ResponseEntity.ok(product);
    }

    /**
     * Unpublish product (move back to READY_FOR_RELEASE)
     * POST /api/products/{id}/unpublish
     */
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ProductResponse> unpublishProduct(@PathVariable String id) {
        ProductResponse product = productService.unpublishProduct(id);
        return ResponseEntity.ok(product);
    }

    /**
     * Archive product
     * PUT /api/products/{id}/archive
     */
    @PutMapping("/{id}/archive")
    public ResponseEntity<ProductResponse> archiveProduct(@PathVariable String id) {
        ProductResponse product = productService.archiveProduct(id);
        return ResponseEntity.ok(product);
    }
}
