package com.mirror.product.controller;

import com.mirror.product.dto.ExchangeRateResponse;
import com.mirror.product.dto.ImportCostRequest;
import com.mirror.product.dto.ImportCostResponse;
import com.mirror.product.service.CurrencyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for currency conversion and import cost calculations
 * Uses FREE exchangerate-api.com API
 */
@RestController
@RequestMapping("/api/currency")
@CrossOrigin(origins = "*")
public class CurrencyController {

    @Autowired
    private CurrencyService currencyService;

    /**
     * Get exchange rate between two currencies
     * GET /api/currency/rate?from=USD&to=VND
     */
    @GetMapping("/rate")
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(
            @RequestParam String from,
            @RequestParam String to) {
        ExchangeRateResponse rate = currencyService.getExchangeRate(from, to);
        return ResponseEntity.ok(rate);
    }

    /**
     * Get all exchange rates for a base currency
     * GET /api/currency/rates/USD
     */
    @GetMapping("/rates/{baseCurrency}")
    public ResponseEntity<List<ExchangeRateResponse>> getAllExchangeRates(
            @PathVariable String baseCurrency) {
        List<ExchangeRateResponse> rates = currencyService.getAllExchangeRates(baseCurrency);
        return ResponseEntity.ok(rates);
    }

    /**
     * Calculate import cost with customs, VAT, and fees
     * POST /api/currency/calculate-import-cost
     *
     * Example request body:
     * {
     *   "costPerUnit": 10.50,
     *   "quantity": 100,
     *   "costCurrency": "USD",
     *   "targetCurrency": "VND",
     *   "customsDutyRate": 15.0,
     *   "vatRate": 10.0,
     *   "shippingCost": 50.00,
     *   "insuranceCost": 25.00
     * }
     */
    @PostMapping("/calculate-import-cost")
    public ResponseEntity<ImportCostResponse> calculateImportCost(
            @Valid @RequestBody ImportCostRequest request) {
        ImportCostResponse response = currencyService.calculateImportCost(request);
        return ResponseEntity.ok(response);
    }
}
