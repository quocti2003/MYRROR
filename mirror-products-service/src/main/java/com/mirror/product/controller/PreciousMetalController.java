package com.mirror.product.controller;

import com.mirror.product.dto.MaterialCostRequest;
import com.mirror.product.dto.MaterialCostResponse;
import com.mirror.product.dto.PreciousMetalPriceResponse;
import com.mirror.product.service.PreciousMetalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for precious metal prices and material cost calculations
 *
 * API INTEGRATION:
 * - Primary: gold-api.com (FREE, no auth required)
 * - Fallback: metals.dev (FREE 100 req/month, API key required)
 * - Emergency: Static mock data
 *
 * See PreciousMetalService for configuration details
 */
@RestController
@RequestMapping("/api/precious-metals")
@CrossOrigin(origins = "*")
public class PreciousMetalController {

    @Autowired
    private PreciousMetalService preciousMetalService;

    /**
     * Get current precious metal prices in USD
     * GET /api/precious-metals/prices/usd
     *
     * Returns prices for: GOLD, SILVER, PLATINUM, PALLADIUM
     * Cached for 15 minutes
     */
    @GetMapping("/prices/usd")
    public ResponseEntity<List<PreciousMetalPriceResponse>> getPricesInUSD() {
        List<PreciousMetalPriceResponse> prices = preciousMetalService.getPricesInUSD();
        return ResponseEntity.ok(prices);
    }

    /**
     * Get current precious metal prices in VND
     * GET /api/precious-metals/prices/vnd
     *
     * Converts USD prices using real-time exchange rates
     * Cached for 15 minutes
     */
    @GetMapping("/prices/vnd")
    public ResponseEntity<List<PreciousMetalPriceResponse>> getPricesInVND() {
        List<PreciousMetalPriceResponse> prices = preciousMetalService.getPricesInVND();
        return ResponseEntity.ok(prices);
    }

    /**
     * Force refresh metal prices (bypass cache)
     * POST /api/precious-metals/refresh
     *
     * Use this to get fresh prices immediately without waiting for cache expiry
     */
    @PostMapping("/refresh")
    public ResponseEntity<List<PreciousMetalPriceResponse>> refreshPrices() {
        List<PreciousMetalPriceResponse> prices = preciousMetalService.refreshPrices();
        return ResponseEntity.ok(prices);
    }

    /**
     * Calculate material cost for a given weight and metal type
     * POST /api/precious-metals/calculate-material-cost
     *
     * Example request body:
     * {
     *   "metalType": "GOLD",
     *   "weight": 5.5,
     *   "weightUnit": "GRAMS",
     *   "currency": "USD",
     *   "quantity": 10
     * }
     */
    @PostMapping("/calculate-material-cost")
    public ResponseEntity<MaterialCostResponse> calculateMaterialCost(
            @Valid @RequestBody MaterialCostRequest request) {
        MaterialCostResponse response = preciousMetalService.calculateMaterialCost(request);
        return ResponseEntity.ok(response);
    }
}
