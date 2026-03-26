package com.mirror.product.controller;

import com.mirror.product.dto.VendorOptimizationRequest;
import com.mirror.product.dto.VendorOptimizationResponse;
import com.mirror.product.service.VendorOptimizationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for vendor optimization using multi-criteria decision analysis
 */
@RestController
@RequestMapping("/api/vendor-optimization")
@CrossOrigin(origins = "*")
public class VendorOptimizationController {

    @Autowired
    private VendorOptimizationService vendorOptimizationService;

    /**
     * Find optimal vendors based on multi-criteria optimization
     * POST /api/vendor-optimization/calculate
     *
     * Example request body:
     * {
     *   "totalPieces": 100,
     *   "maxBudget": 10000.00,
     *   "deadline": "2025-12-31",
     *   "productCategory": "Ring",
     *   "material": "Gold",
     *   "qualityRequirement": 4,
     *   "costWeight": 0.40,
     *   "qualityWeight": 0.35,
     *   "timelineWeight": 0.25
     * }
     */
    @PostMapping("/calculate")
    public ResponseEntity<List<VendorOptimizationResponse>> calculateOptimalVendors(
            @Valid @RequestBody VendorOptimizationRequest request) {
        List<VendorOptimizationResponse> optimizations =
            vendorOptimizationService.findOptimalVendors(request);
        return ResponseEntity.ok(optimizations);
    }
}
