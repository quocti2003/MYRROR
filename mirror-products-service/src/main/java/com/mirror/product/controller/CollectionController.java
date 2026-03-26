package com.mirror.product.controller;

import com.mirror.product.dto.CollectionRequest;
import com.mirror.product.dto.CollectionResponse;
import com.mirror.product.dto.CollectionProductRequest;
import com.mirror.product.dto.ProductResponse;
import com.mirror.product.service.CollectionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    /**
     * Get all active collections
     * GET /api/collections
     */
    @GetMapping
    public ResponseEntity<List<CollectionResponse>> getAllCollections() {
        List<CollectionResponse> collections = collectionService.getAllActiveCollections();
        return ResponseEntity.ok(collections);
    }

    /**
     * Get all collections including inactive (admin)
     * GET /api/collections/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<CollectionResponse>> getAllCollectionsIncludingInactive() {
        List<CollectionResponse> collections = collectionService.getAllCollections();
        return ResponseEntity.ok(collections);
    }

    /**
     * Get collection by ID
     * GET /api/collections/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CollectionResponse> getCollectionById(@PathVariable String id) {
        return collectionService.getCollectionById(id)
                .map(collection -> ResponseEntity.ok(collection))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get collection by name
     * GET /api/collections/name/{name}
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<CollectionResponse> getCollectionByName(@PathVariable String name) {
        return collectionService.getCollectionByName(name)
                .map(collection -> ResponseEntity.ok(collection))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all active collections with products
     * GET /api/collections/with-products
     */
    @GetMapping("/with-products")
    public ResponseEntity<List<CollectionResponse>> getAllCollectionsWithProducts() {
        List<CollectionResponse> collections = collectionService.getAllCollectionsWithProducts();
        return ResponseEntity.ok(collections);
    }
    
    /**
     * Get collection with products
     * GET /api/collections/{id}/products
     */
    @GetMapping("/{id}/products")
    public ResponseEntity<CollectionResponse> getCollectionWithProducts(@PathVariable String id) {
        return collectionService.getCollectionWithProducts(id)
                .map(collection -> ResponseEntity.ok(collection))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get products in collection (products only)
     * GET /api/collections/{id}/products-list
     */
    @GetMapping("/{id}/products-list")
    public ResponseEntity<List<ProductResponse>> getProductsInCollection(@PathVariable String id) {
        List<ProductResponse> products = collectionService.getProductsInCollection(id);
        return ResponseEntity.ok(products);
    }

    /**
     * Get featured collections
     * GET /api/collections/featured
     */
    @GetMapping("/featured")
    public ResponseEntity<List<CollectionResponse>> getFeaturedCollections() {
        List<CollectionResponse> collections = collectionService.getFeaturedCollections();
        return ResponseEntity.ok(collections);
    }

    /**
     * Get collections by year
     * GET /api/collections/year/{year}
     */
    @GetMapping("/year/{year}")
    public ResponseEntity<List<CollectionResponse>> getCollectionsByYear(@PathVariable Integer year) {
        List<CollectionResponse> collections = collectionService.getCollectionsByYear(year);
        return ResponseEntity.ok(collections);
    }

    /**
     * Get collections by season and year
     * GET /api/collections/season/{season}/year/{year}
     */
    @GetMapping("/season/{season}/year/{year}")
    public ResponseEntity<List<CollectionResponse>> getCollectionsBySeasonAndYear(
            @PathVariable String season, @PathVariable Integer year) {
        List<CollectionResponse> collections = collectionService.getCollectionsBySeasonAndYear(season, year);
        return ResponseEntity.ok(collections);
    }

    /**
     * Search collections
     * GET /api/collections/search?q={searchTerm}
     */
    @GetMapping("/search")
    public ResponseEntity<List<CollectionResponse>> searchCollections(@RequestParam("q") String searchTerm) {
        List<CollectionResponse> collections = collectionService.searchCollections(searchTerm);
        return ResponseEntity.ok(collections);
    }

    /**
     * Get filter options for collections
     * GET /api/collections/filters
     */
    @GetMapping("/filters")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        Map<String, Object> filters = collectionService.getFilterOptions();
        return ResponseEntity.ok(filters);
    }

    /**
     * Get collection statistics
     * GET /api/collections/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getCollectionStatistics() {
        Map<String, Object> statistics = collectionService.getCollectionStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Health check endpoint
     * GET /api/collections/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        long totalCollections = (Long) collectionService.getCollectionStatistics()
                .getOrDefault("totalActiveCollections", 0L);

        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "service", "CollectionService",
            "totalActiveCollections", totalCollections,
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Create a new collection
     * POST /api/collections
     */
    @PostMapping
    public ResponseEntity<CollectionResponse> createCollection(@Valid @RequestBody CollectionRequest request) {
        try {
            CollectionResponse createdCollection = collectionService.createCollection(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCollection);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing collection
     * PUT /api/collections/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CollectionResponse> updateCollection(
            @PathVariable String id,
            @Valid @RequestBody CollectionRequest request) {
        try {
            return collectionService.updateCollection(id, request)
                    .map(collection -> ResponseEntity.ok(collection))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a collection (soft delete - mark as ARCHIVED)
     * DELETE /api/collections/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCollection(@PathVariable String id) {
        boolean deleted = collectionService.deleteCollection(id);
        
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                "message", "Collection archived successfully",
                "id", id,
                "status", "ARCHIVED"
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Hard delete a collection (permanent removal)
     * DELETE /api/collections/{id}/permanent
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Map<String, Object>> hardDeleteCollection(@PathVariable String id) {
        boolean deleted = collectionService.hardDeleteCollection(id);
        
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                "message", "Collection permanently deleted",
                "id", id
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // COLLECTION-PRODUCT MANAGEMENT ENDPOINTS

    /**
     * Add a product to collection
     * POST /api/collections/{collectionId}/products
     */
    @PostMapping("/{collectionId}/products")
    public ResponseEntity<Map<String, Object>> addProductToCollection(
            @PathVariable String collectionId,
            @Valid @RequestBody CollectionProductRequest request) {
        try {
            request.setCollectionId(collectionId); // Ensure collectionId matches path variable
            boolean added = collectionService.addProductToCollection(request);
            
            if (added) {
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Product added to collection successfully",
                    "collectionId", collectionId,
                    "productId", request.getProductId()
                ));
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Remove a product from collection
     * DELETE /api/collections/{collectionId}/products/{productId}
     */
    @DeleteMapping("/{collectionId}/products/{productId}")
    public ResponseEntity<Map<String, Object>> removeProductFromCollection(
            @PathVariable String collectionId,
            @PathVariable String productId) {
        
        boolean removed = collectionService.removeProductFromCollection(collectionId, productId);
        
        if (removed) {
            return ResponseEntity.ok(Map.of(
                "message", "Product removed from collection successfully",
                "collectionId", collectionId,
                "productId", productId
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update product position in collection
     * PUT /api/collections/{collectionId}/products/{productId}/sort-order
     */
    @PutMapping("/{collectionId}/products/{productId}/sort-order")
    public ResponseEntity<Map<String, Object>> updateProductSortOrder(
            @PathVariable String collectionId,
            @PathVariable String productId,
            @RequestBody Map<String, Integer> request) {
        
        Integer sortOrder = request.get("sortOrder");
        if (sortOrder == null) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean updated = collectionService.updateProductSortOrder(collectionId, productId, sortOrder);
        
        if (updated) {
            return ResponseEntity.ok(Map.of(
                "message", "Product sort order updated successfully",
                "collectionId", collectionId,
                "productId", productId,
                "sortOrder", sortOrder
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Toggle hero product status
     * POST /api/collections/{collectionId}/products/{productId}/toggle-hero
     */
    @PostMapping("/{collectionId}/products/{productId}/toggle-hero")
    public ResponseEntity<Map<String, Object>> toggleHeroProduct(
            @PathVariable String collectionId,
            @PathVariable String productId) {
        
        Boolean isHero = collectionService.toggleHeroProduct(collectionId, productId);
        
        if (isHero != null) {
            return ResponseEntity.ok(Map.of(
                "message", "Hero product status updated",
                "collectionId", collectionId,
                "productId", productId,
                "isHeroProduct", isHero
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}