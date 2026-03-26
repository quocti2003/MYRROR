package com.mirror.product.controller;

import com.mirror.product.dto.productops.*;
import com.mirror.product.service.ProductOpsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ProductOpsController - Product Operations Dashboard API
 *
 * Manages the 6-step product workflow:
 * 1. Create Draft Product
 * 2. Fill Assets & Product Data
 * 3. Request MISA SKU Creation
 * 4. MISA SKU Synced
 * 5. Mark Product Ready
 * 6. Publish to Website
 */
@RestController
@RequestMapping("/api/product-ops")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ProductOpsController {

    private final ProductOpsService productOpsService;

    // ==================== DASHBOARD & OVERVIEW ====================

    /**
     * Get all products with workflow status for Product Ops Dashboard
     * GET /api/product-ops/products
     */
    @GetMapping("/products")
    public ResponseEntity<List<ProductWorkflowResponse>> getAllProductsWithWorkflow(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        log.info("Getting products with workflow - status: {}, search: {}", status, search);
        List<ProductWorkflowResponse> products = productOpsService.getAllProductsWithWorkflow(status, search);
        return ResponseEntity.ok(products);
    }

    /**
     * Get product workflow summary statistics
     * GET /api/product-ops/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<WorkflowSummaryResponse> getWorkflowSummary() {
        WorkflowSummaryResponse summary = productOpsService.getWorkflowSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Get product by ID with full workflow details
     * GET /api/product-ops/products/{id}
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductWorkflowResponse> getProductWorkflow(@PathVariable String id) {
        return productOpsService.getProductWorkflow(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== STEP 1: CREATE DRAFT PRODUCT ====================

    /**
     * Create draft product with internal SKU
     * POST /api/product-ops/create-draft
     */
    @PostMapping("/create-draft")
    public ResponseEntity<ProductWorkflowResponse> createDraftProduct(
            @Valid @RequestBody CreateDraftRequest request) {

        log.info("Creating draft product - name: {}, SKU: {}",
                request.getName(), request.getInternalSKU());

        ProductWorkflowResponse response = productOpsService.createDraftProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== STEP 3: FILL ASSETS & PRODUCT DATA ====================

    /**
     * Update product with assets and data
     * PUT /api/product-ops/products/{id}/fulfillment
     */
    @PutMapping("/products/{id}/fulfillment")
    public ResponseEntity<ProductWorkflowResponse> updateProductFulfillment(
            @PathVariable String id,
            @Valid @RequestBody ProductFulfillmentRequest request) {

        log.info("Updating product fulfillment - id: {}", id);

        return productOpsService.updateProductFulfillment(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== STEP 4-5: MISA INTEGRATION ====================

    /**
     * Get products filtered by MISA sync status
     * GET /api/product-ops/misa/products
     */
    @GetMapping("/misa/products")
    public ResponseEntity<List<MISAProductResponse>> getMISAProducts(
            @RequestParam(required = false) String status) {

        log.info("Getting MISA products - status: {}", status);
        List<MISAProductResponse> products = productOpsService.getMISAProducts(status);
        return ResponseEntity.ok(products);
    }

    /**
     * Request MISA SKU creation for a product
     * POST /api/product-ops/products/{id}/request-misa-sku
     */
    @PostMapping("/products/{id}/request-misa-sku")
    public ResponseEntity<MISARequestResponse> requestMISASKU(@PathVariable String id) {
        log.info("Requesting MISA SKU for product: {}", id);

        MISARequestResponse response = productOpsService.requestMISASKU(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retry failed MISA sync
     * POST /api/product-ops/products/{id}/retry-misa-sync
     */
    @PostMapping("/products/{id}/retry-misa-sync")
    public ResponseEntity<MISARequestResponse> retryMISASync(@PathVariable String id) {
        log.info("Retrying MISA sync for product: {}", id);

        MISARequestResponse response = productOpsService.retryMISASync(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update MISA SKU for a product (callback from MISA system)
     * PUT /api/product-ops/products/{id}/misa-sku
     */
    @PutMapping("/products/{id}/misa-sku")
    public ResponseEntity<ProductWorkflowResponse> updateMISASKU(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {

        String misaSKU = request.get("misaSKU");
        log.info("Updating MISA SKU for product: {} - SKU: {}", id, misaSKU);

        return productOpsService.updateMISASKU(id, misaSKU)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get MISA sync history for a product
     * GET /api/product-ops/products/{id}/misa-history
     */
    @GetMapping("/products/{id}/misa-history")
    public ResponseEntity<List<MISAHistoryResponse>> getMISAHistory(@PathVariable String id) {
        List<MISAHistoryResponse> history = productOpsService.getMISAHistory(id);
        return ResponseEntity.ok(history);
    }

    // ==================== STEP 6-7: MARK READY & PUBLISH ====================

    /**
     * Get products ready for publication
     * GET /api/product-ops/ready-products
     */
    @GetMapping("/ready-products")
    public ResponseEntity<List<ProductWorkflowResponse>> getReadyProducts() {
        List<ProductWorkflowResponse> products = productOpsService.getReadyProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Get published products
     * GET /api/product-ops/published-products
     */
    @GetMapping("/published-products")
    public ResponseEntity<List<ProductWorkflowResponse>> getPublishedProducts() {
        List<ProductWorkflowResponse> products = productOpsService.getPublishedProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Mark product as ready for publication
     * POST /api/product-ops/products/{id}/mark-ready
     */
    @PostMapping("/products/{id}/mark-ready")
    public ResponseEntity<ProductWorkflowResponse> markProductReady(@PathVariable String id) {
        log.info("Marking product ready: {}", id);

        return productOpsService.markProductReady(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Publish product to website
     * POST /api/product-ops/products/{id}/publish
     */
    @PostMapping("/products/{id}/publish")
    public ResponseEntity<ProductWorkflowResponse> publishProduct(@PathVariable String id) {
        log.info("Publishing product: {}", id);

        return productOpsService.publishProduct(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Unpublish product from website
     * POST /api/product-ops/products/{id}/unpublish
     */
    @PostMapping("/products/{id}/unpublish")
    public ResponseEntity<ProductWorkflowResponse> unpublishProduct(@PathVariable String id) {
        log.info("Unpublishing product: {}", id);

        return productOpsService.unpublishProduct(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== WORKFLOW ACTIONS ====================

    /**
     * Update workflow step status
     * PUT /api/product-ops/products/{id}/workflow-step
     */
    @PutMapping("/products/{id}/workflow-step")
    public ResponseEntity<ProductWorkflowResponse> updateWorkflowStep(
            @PathVariable String id,
            @Valid @RequestBody WorkflowStepUpdateRequest request) {

        log.info("Updating workflow step for product: {} - step: {}, status: {}",
                id, request.getStepId(), request.getStatus());

        return productOpsService.updateWorkflowStep(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get workflow history for a product
     * GET /api/product-ops/products/{id}/workflow-history
     */
    @GetMapping("/products/{id}/workflow-history")
    public ResponseEntity<List<WorkflowHistoryResponse>> getWorkflowHistory(@PathVariable String id) {
        List<WorkflowHistoryResponse> history = productOpsService.getWorkflowHistory(id);
        return ResponseEntity.ok(history);
    }

    // ==================== HEALTH CHECK ====================

    /**
     * Health check endpoint
     * GET /api/product-ops/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        WorkflowSummaryResponse summary = productOpsService.getWorkflowSummary();

        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "service", "ProductOpsService",
            "timestamp", System.currentTimeMillis(),
            "summary", Map.of(
                "draft", summary.getDraft(),
                "inProgress", summary.getInProgress(),
                "ready", summary.getReady(),
                "published", summary.getPublished()
            )
        ));
    }
}
