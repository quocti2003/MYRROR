package com.mirror.product.controller;

import com.mirror.product.dto.VendorMatchRequest;
import com.mirror.product.dto.VendorMatchResponse;
import com.mirror.product.service.VendorMatchingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for vendor matching and selection
 */
@RestController
@RequestMapping("/api/vendor-matching")
@CrossOrigin(origins = "*")
public class VendorMatchingController {

    @Autowired
    private VendorMatchingService vendorMatchingService;

    /**
     * Find matching vendors for a collection plan
     * POST /api/vendor-matching/find
     *
     * Example request body:
     * {
     *   "collectionPlanId": 123,
     *   "productCategory": "Ring",
     *   "material": "Gold",
     *   "minQuantity": 100,
     *   "maxBudget": 5000.00,
     *   "deadline": "2025-12-31",
     *   "requiredCapabilities": ["Custom Design", "Mass Production"]
     * }
     */
    @PostMapping("/find")
    public ResponseEntity<List<VendorMatchResponse>> findMatchingVendors(
            @Valid @RequestBody VendorMatchRequest request) {
        List<VendorMatchResponse> matches = vendorMatchingService.findMatchingVendors(request);
        return ResponseEntity.ok(matches);
    }

    /**
     * Get vendor matches by collection plan ID
     * GET /api/vendor-matching/collection-plan/{id}
     */
    @GetMapping("/collection-plan/{id}")
    public ResponseEntity<List<VendorMatchResponse>> getMatchesByCollectionPlan(
            @PathVariable Long id) {
        VendorMatchRequest request = new VendorMatchRequest();
        request.setCollectionPlanId(id);

        List<VendorMatchResponse> matches = vendorMatchingService.findMatchingVendors(request);
        return ResponseEntity.ok(matches);
    }
}
