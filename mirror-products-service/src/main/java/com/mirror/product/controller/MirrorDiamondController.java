package com.mirror.product.controller;

import com.mirror.product.dto.diamond.*;
import com.mirror.product.service.MirrorDiamondService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Mirror Diamond operations
 * Base URL: /api/diamonds
 */
@RestController
@RequestMapping("/api/diamonds")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class MirrorDiamondController {

    private final MirrorDiamondService diamondService;

    // ==================== SKU GENERATION ====================

    /**
     * Generate Mirror SKU code for a diamond
     * POST /api/diamonds/generate-sku
     */
    @PostMapping("/generate-sku")
    public ResponseEntity<DiamondSkuResponse> generateSku(
            @Valid @RequestBody DiamondSkuRequest request) {

        log.info("Generating SKU for diamond: {} {} {}ct",
                request.getColor(), request.getClarity(), request.getCaratWeight());

        DiamondSkuResponse response = diamondService.generateSku(request);
        return ResponseEntity.ok(response);
    }

    // ==================== CRUD OPERATIONS ====================

    /**
     * Create a new lab-grown diamond
     * POST /api/diamonds
     */
    @PostMapping
    public ResponseEntity<DiamondResponse> createDiamond(
            @Valid @RequestBody DiamondRequest request) {

        log.info("Creating diamond: {} {} {}ct",
                request.getColor(), request.getClarity(), request.getCaratWeight());

        DiamondResponse response = diamondService.createDiamond(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get diamond by ID
     * GET /api/diamonds/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DiamondResponse> getDiamondById(@PathVariable String id) {
        log.info("Getting diamond by ID: {}", id);

        return diamondService.getDiamondById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get diamond by SKU code
     * GET /api/diamonds/sku/{skuCode}
     */
    @GetMapping("/sku/{skuCode}")
    public ResponseEntity<DiamondResponse> getDiamondBySkuCode(@PathVariable String skuCode) {
        log.info("Getting diamond by SKU: {}", skuCode);

        return diamondService.getDiamondBySkuCode(skuCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get diamond by certificate number
     * GET /api/diamonds/cert/{certNumber}
     */
    @GetMapping("/cert/{certNumber}")
    public ResponseEntity<DiamondResponse> getDiamondByCertNumber(@PathVariable String certNumber) {
        log.info("Getting diamond by cert number: {}", certNumber);

        return diamondService.getDiamondByCertNumber(certNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all active diamonds
     * GET /api/diamonds
     */
    @GetMapping
    public ResponseEntity<List<DiamondResponse>> getAllActiveDiamonds() {
        log.info("Getting all active diamonds");

        List<DiamondResponse> diamonds = diamondService.getAllActiveDiamonds();
        return ResponseEntity.ok(diamonds);
    }

    /**
     * Update diamond
     * PUT /api/diamonds/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<DiamondResponse> updateDiamond(
            @PathVariable String id,
            @Valid @RequestBody DiamondRequest request) {

        log.info("Updating diamond: {}", id);

        return diamondService.updateDiamond(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update diamond status
     * PATCH /api/diamonds/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateDiamondStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");
        log.info("Updating diamond status: {} → {}", id, status);

        boolean updated = diamondService.updateDiamondStatus(id, status);

        if (updated) {
            return ResponseEntity.ok(Map.of(
                    "message", "Diamond status updated successfully",
                    "status", status
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete diamond (soft delete)
     * DELETE /api/diamonds/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDiamond(@PathVariable String id) {
        log.info("Deleting diamond: {}", id);

        boolean deleted = diamondService.deleteDiamond(id);

        if (deleted) {
            return ResponseEntity.ok(Map.of(
                    "message", "Diamond deleted successfully"
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== SEARCH & FILTER ====================

    /**
     * Search diamonds with filters
     * POST /api/diamonds/search
     */
    @PostMapping("/search")
    public ResponseEntity<List<DiamondResponse>> searchDiamonds(
            @RequestBody DiamondSearchRequest request) {

        log.info("Searching diamonds: {}", request);

        List<DiamondResponse> diamonds = diamondService.searchDiamonds(request);
        return ResponseEntity.ok(diamonds);
    }

    /**
     * Get diamonds by invoice number
     * GET /api/diamonds/invoice/{invoiceNumber}
     */
    @GetMapping("/invoice/{invoiceNumber}")
    public ResponseEntity<List<DiamondResponse>> getDiamondsByInvoice(
            @PathVariable String invoiceNumber) {

        log.info("Getting diamonds for invoice: {}", invoiceNumber);

        List<DiamondResponse> diamonds = diamondService.getDiamondsByInvoice(invoiceNumber);
        return ResponseEntity.ok(diamonds);
    }

    /**
     * Get diamonds by status
     * GET /api/diamonds/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DiamondResponse>> getDiamondsByStatus(
            @PathVariable String status) {

        log.info("Getting diamonds with status: {}", status);

        List<DiamondResponse> diamonds = diamondService.getDiamondsByStatus(status);
        return ResponseEntity.ok(diamonds);
    }

    // ==================== STATISTICS ====================

    /**
     * Get inventory statistics
     * GET /api/diamonds/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getInventoryStats() {
        log.info("Getting inventory statistics");

        long inStock = diamondService.countByStatus("IN_STOCK");
        long reserved = diamondService.countByStatus("RESERVED");
        long sold = diamondService.countByStatus("SOLD");
        BigDecimal totalCarats = diamondService.getTotalCaratWeightInStock();
        BigDecimal totalValue = diamondService.getTotalInventoryValueUsd();

        return ResponseEntity.ok(Map.of(
                "inStock", inStock,
                "reserved", reserved,
                "sold", sold,
                "totalCaratWeight", totalCarats,
                "totalInventoryValueUsd", totalValue
        ));
    }
}
