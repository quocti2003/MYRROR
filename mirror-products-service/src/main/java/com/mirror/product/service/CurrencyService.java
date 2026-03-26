package com.mirror.product.service;

import com.mirror.product.dto.ExchangeRateResponse;
import com.mirror.product.dto.ImportCostRequest;
import com.mirror.product.dto.ImportCostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for currency conversion and import cost calculations
 * Uses FREE exchangerate-api.com API for real-time exchange rates
 */
@Service
public class CurrencyService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);

    @Value("${currency.api.url:https://api.exchangerate-api.com/v4/latest}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public CurrencyService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get exchange rate between two currencies
     * Cached for 1 hour to reduce API calls
     */
    @Cacheable(value = "exchangeRates", key = "#fromCurrency + '_' + #toCurrency")
    public ExchangeRateResponse getExchangeRate(String fromCurrency, String toCurrency) {
        try {
            // Call FREE exchangerate-api.com
            String url = apiUrl + "/" + fromCurrency.toUpperCase();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("rates")) {
                throw new RuntimeException("Failed to fetch exchange rates");
            }

            @SuppressWarnings("unchecked")
            Map<String, Double> rates = (Map<String, Double>) response.get("rates");

            Double rate = rates.get(toCurrency.toUpperCase());
            if (rate == null) {
                throw new RuntimeException("Currency " + toCurrency + " not found");
            }

            ExchangeRateResponse exchangeRate = new ExchangeRateResponse();
            exchangeRate.setFromCurrency(fromCurrency.toUpperCase());
            exchangeRate.setToCurrency(toCurrency.toUpperCase());
            exchangeRate.setRate(BigDecimal.valueOf(rate));
            exchangeRate.setSource("exchangerate-api.com");
            exchangeRate.setTimestamp(LocalDateTime.now());

            logger.info("Fetched exchange rate: {} {} = {} {}",
                1, fromCurrency, rate, toCurrency);

            return exchangeRate;

        } catch (Exception e) {
            logger.error("Error fetching exchange rate from {} to {}: {}",
                fromCurrency, toCurrency, e.getMessage());
            throw new RuntimeException("Failed to fetch exchange rate: " + e.getMessage(), e);
        }
    }

    /**
     * Get all available exchange rates for a base currency
     */
    @Cacheable(value = "allExchangeRates", key = "#baseCurrency")
    public List<ExchangeRateResponse> getAllExchangeRates(String baseCurrency) {
        try {
            String url = apiUrl + "/" + baseCurrency.toUpperCase();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("rates")) {
                throw new RuntimeException("Failed to fetch exchange rates");
            }

            @SuppressWarnings("unchecked")
            Map<String, Double> rates = (Map<String, Double>) response.get("rates");

            return rates.entrySet().stream()
                .map(entry -> {
                    ExchangeRateResponse rate = new ExchangeRateResponse();
                    rate.setFromCurrency(baseCurrency.toUpperCase());
                    rate.setToCurrency(entry.getKey());
                    rate.setRate(BigDecimal.valueOf(entry.getValue()));
                    rate.setSource("exchangerate-api.com");
                    rate.setTimestamp(LocalDateTime.now());
                    return rate;
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error fetching all exchange rates for {}: {}",
                baseCurrency, e.getMessage());
            throw new RuntimeException("Failed to fetch exchange rates: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate import cost with customs, VAT, and other fees
     */
    public ImportCostResponse calculateImportCost(ImportCostRequest request) {
        try {
            // Get exchange rate
            ExchangeRateResponse exchangeRate = getExchangeRate(
                request.getCostCurrency(),
                request.getTargetCurrency()
            );

            // Calculate subtotal in original currency
            BigDecimal subtotalOriginal = request.getCostPerUnit()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

            // Convert to target currency
            BigDecimal subtotalConverted = subtotalOriginal
                .multiply(exchangeRate.getRate())
                .setScale(2, RoundingMode.HALF_UP);

            // Calculate customs duty (percentage of subtotal)
            BigDecimal customsDuty = subtotalConverted
                .multiply(request.getCustomsDutyRate().divide(BigDecimal.valueOf(100)))
                .setScale(2, RoundingMode.HALF_UP);

            // Calculate VAT (percentage of subtotal + customs duty)
            BigDecimal taxableAmount = subtotalConverted.add(customsDuty);
            BigDecimal vat = taxableAmount
                .multiply(request.getVatRate().divide(BigDecimal.valueOf(100)))
                .setScale(2, RoundingMode.HALF_UP);

            // Convert shipping and insurance costs if provided
            BigDecimal shippingCost = BigDecimal.ZERO;
            BigDecimal insuranceCost = BigDecimal.ZERO;

            if (request.getShippingCost() != null) {
                shippingCost = request.getShippingCost()
                    .multiply(exchangeRate.getRate())
                    .setScale(2, RoundingMode.HALF_UP);
            }

            if (request.getInsuranceCost() != null) {
                insuranceCost = request.getInsuranceCost()
                    .multiply(exchangeRate.getRate())
                    .setScale(2, RoundingMode.HALF_UP);
            }

            // Calculate total cost
            BigDecimal totalCost = subtotalConverted
                .add(customsDuty)
                .add(vat)
                .add(shippingCost)
                .add(insuranceCost)
                .setScale(2, RoundingMode.HALF_UP);

            // Calculate cost per unit after all fees
            BigDecimal costPerUnit = totalCost
                .divide(BigDecimal.valueOf(request.getQuantity()), 2, RoundingMode.HALF_UP);

            // Build response
            ImportCostResponse response = new ImportCostResponse();
            response.setSubtotal(subtotalConverted);
            response.setCustomsDuty(customsDuty);
            response.setVat(vat);
            response.setShippingCost(shippingCost);
            response.setInsuranceCost(insuranceCost);
            response.setTotalCost(totalCost);
            response.setCostPerUnit(costPerUnit);
            response.setCurrency(request.getTargetCurrency());
            response.setExchangeRate(exchangeRate.getRate());
            response.setExchangeRateSource(exchangeRate.getSource());
            response.setCalculatedAt(LocalDateTime.now());

            logger.info("Calculated import cost: {} {} -> {} {} (total: {} {})",
                subtotalOriginal, request.getCostCurrency(),
                subtotalConverted, request.getTargetCurrency(),
                totalCost, request.getTargetCurrency());

            return response;

        } catch (Exception e) {
            logger.error("Error calculating import cost: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate import cost: " + e.getMessage(), e);
        }
    }
}
