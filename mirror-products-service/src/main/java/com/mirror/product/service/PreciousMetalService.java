package com.mirror.product.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.dto.MaterialCostRequest;
import com.mirror.product.dto.MaterialCostResponse;
import com.mirror.product.dto.PreciousMetalPriceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for precious metal price tracking and material cost calculations
 *
 * API INTEGRATION STRATEGY:
 * =========================
 * Primary:   gold-api.com (FREE, no auth, no rate limits)
 * Fallback:  metals.dev (FREE 100 req/month, API key required)
 * Emergency: Static mock data
 *
 * Caching: 15 minutes TTL to reduce API calls and improve performance
 */
@Service
public class PreciousMetalService {

    private static final Logger logger = LoggerFactory.getLogger(PreciousMetalService.class);

    // Weight conversion constants
    private static final BigDecimal GRAMS_PER_OUNCE = BigDecimal.valueOf(31.1035);
    private static final BigDecimal GRAMS_PER_LUONG = BigDecimal.valueOf(37.5);
    private static final BigDecimal GRAMS_PER_CHI = BigDecimal.valueOf(3.75);

    // API URLs
    private static final String GOLD_API_BASE_URL = "https://api.gold-api.com/price";
    private static final String METALS_DEV_BASE_URL = "https://api.metals.dev/v1/latest";

    // Metal symbols mapping
    private static final Map<String, String> GOLD_API_SYMBOLS = Map.of(
        "GOLD", "XAU",
        "SILVER", "XAG",
        "PLATINUM", "XPT",
        "PALLADIUM", "XPD"
    );

    @Value("${metals.dev.api.key:}")
    private String metalsDevApiKey;

    @Value("${precious.metal.use-mock-data:false}")
    private boolean useMockData;

    @Autowired
    private CurrencyService currencyService;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Last known prices cache for emergency fallback
    private final Map<String, PreciousMetalPriceResponse> lastKnownPrices = new ConcurrentHashMap<>();

    public PreciousMetalService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get precious metal prices in USD
     * Cached for 15 minutes to reduce API calls
     */
    @Cacheable(value = "metalPrices", key = "'USD'")
    public List<PreciousMetalPriceResponse> getPricesInUSD() {
        if (useMockData) {
            logger.info("Using mock data (configured via precious.metal.use-mock-data=true)");
            return getMockPricesUSD();
        }

        List<PreciousMetalPriceResponse> prices = new ArrayList<>();

        // Try primary API (gold-api.com) for each metal
        for (String metal : List.of("GOLD", "SILVER", "PLATINUM", "PALLADIUM")) {
            PreciousMetalPriceResponse price = fetchFromGoldApi(metal);

            if (price == null) {
                // Try fallback API (metals.dev)
                price = fetchFromMetalsDev(metal);
            }

            if (price == null) {
                // Use last known price or mock
                price = getLastKnownOrMock(metal);
            }

            if (price != null) {
                prices.add(price);
                lastKnownPrices.put(metal, price); // Update cache
            }
        }

        if (prices.isEmpty()) {
            logger.warn("All APIs failed, using mock data");
            return getMockPricesUSD();
        }

        return prices;
    }

    /**
     * Fetch price from gold-api.com (PRIMARY - FREE, no auth)
     */
    private PreciousMetalPriceResponse fetchFromGoldApi(String metalType) {
        String symbol = GOLD_API_SYMBOLS.get(metalType);
        if (symbol == null) {
            logger.warn("Unknown metal type for gold-api.com: {}", metalType);
            return null;
        }

        try {
            String url = GOLD_API_BASE_URL + "/" + symbol;
            logger.debug("Fetching {} price from gold-api.com: {}", metalType, url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                // Parse gold-api.com response
                // Expected format: { "price": 2050.50, "symbol": "XAU", "currency": "USD", ... }
                BigDecimal pricePerOunce = null;

                if (root.has("price")) {
                    pricePerOunce = new BigDecimal(root.get("price").asText());
                } else if (root.has("price_gram_24k")) {
                    // Alternative format
                    BigDecimal pricePerGram = new BigDecimal(root.get("price_gram_24k").asText());
                    pricePerOunce = pricePerGram.multiply(GRAMS_PER_OUNCE);
                }

                if (pricePerOunce != null && pricePerOunce.compareTo(BigDecimal.ZERO) > 0) {
                    PreciousMetalPriceResponse price = new PreciousMetalPriceResponse();
                    price.setMetalType(metalType);
                    price.setPricePerOunce(pricePerOunce.setScale(2, RoundingMode.HALF_UP));
                    price.setPricePerGram(pricePerOunce.divide(GRAMS_PER_OUNCE, 4, RoundingMode.HALF_UP));
                    price.setCurrency("USD");
                    price.setSource("gold-api.com");
                    price.setTimestamp(LocalDateTime.now());

                    // Parse 24h change if available
                    if (root.has("ch") || root.has("change")) {
                        JsonNode changeNode = root.has("ch") ? root.get("ch") : root.get("change");
                        price.setChange24h(new BigDecimal(changeNode.asText()));
                    }
                    if (root.has("chp") || root.has("change_percent")) {
                        JsonNode changePercentNode = root.has("chp") ? root.get("chp") : root.get("change_percent");
                        price.setChangePercent24h(new BigDecimal(changePercentNode.asText()));
                    }

                    logger.info("Successfully fetched {} price from gold-api.com: ${}/oz",
                        metalType, pricePerOunce);
                    return price;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch {} from gold-api.com: {}", metalType, e.getMessage());
        }

        return null;
    }

    /**
     * Fetch price from metals.dev (FALLBACK - 100 free requests/month)
     */
    private PreciousMetalPriceResponse fetchFromMetalsDev(String metalType) {
        if (metalsDevApiKey == null || metalsDevApiKey.isEmpty()) {
            logger.debug("metals.dev API key not configured, skipping fallback");
            return null;
        }

        try {
            String symbol = GOLD_API_SYMBOLS.get(metalType);
            String url = METALS_DEV_BASE_URL + "?api_key=" + metalsDevApiKey + "&base=USD&symbols=" + symbol;

            logger.debug("Fetching {} price from metals.dev", metalType);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                // metals.dev returns rates as 1 USD = X metal (inverted)
                // So we need to invert: price = 1 / rate
                if (root.has("metals") && root.get("metals").has(symbol)) {
                    BigDecimal rate = new BigDecimal(root.get("metals").get(symbol).asText());
                    BigDecimal pricePerOunce = BigDecimal.ONE.divide(rate, 2, RoundingMode.HALF_UP);

                    PreciousMetalPriceResponse price = new PreciousMetalPriceResponse();
                    price.setMetalType(metalType);
                    price.setPricePerOunce(pricePerOunce);
                    price.setPricePerGram(pricePerOunce.divide(GRAMS_PER_OUNCE, 4, RoundingMode.HALF_UP));
                    price.setCurrency("USD");
                    price.setSource("metals.dev");
                    price.setTimestamp(LocalDateTime.now());

                    logger.info("Successfully fetched {} price from metals.dev: ${}/oz",
                        metalType, pricePerOunce);
                    return price;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch {} from metals.dev: {}", metalType, e.getMessage());
        }

        return null;
    }

    /**
     * Get last known price or fall back to mock data
     */
    private PreciousMetalPriceResponse getLastKnownOrMock(String metalType) {
        PreciousMetalPriceResponse lastKnown = lastKnownPrices.get(metalType);
        if (lastKnown != null) {
            logger.info("Using last known price for {}: ${}/oz (from {})",
                metalType, lastKnown.getPricePerOunce(), lastKnown.getSource());
            // Mark as cached
            lastKnown.setSource(lastKnown.getSource() + " (cached)");
            return lastKnown;
        }

        // Return mock for this specific metal
        return getMockPrice(metalType);
    }

    /**
     * Get precious metal prices in VND
     */
    @Cacheable(value = "metalPrices", key = "'VND'")
    public List<PreciousMetalPriceResponse> getPricesInVND() {
        try {
            // Get USD prices
            List<PreciousMetalPriceResponse> usdPrices = getPricesInUSD();

            // Get USD to VND exchange rate
            BigDecimal exchangeRate = currencyService.getExchangeRate("USD", "VND").getRate();

            // Convert to VND
            List<PreciousMetalPriceResponse> vndPrices = new ArrayList<>();
            for (PreciousMetalPriceResponse usdPrice : usdPrices) {
                PreciousMetalPriceResponse vndPrice = new PreciousMetalPriceResponse();
                vndPrice.setMetalType(usdPrice.getMetalType());
                vndPrice.setPricePerOunce(usdPrice.getPricePerOunce().multiply(exchangeRate)
                    .setScale(0, RoundingMode.HALF_UP));
                vndPrice.setPricePerGram(usdPrice.getPricePerGram().multiply(exchangeRate)
                    .setScale(0, RoundingMode.HALF_UP));
                vndPrice.setCurrency("VND");
                vndPrice.setExchangeRate(exchangeRate);
                vndPrice.setSource(usdPrice.getSource() + " + exchangerate-api.com");
                vndPrice.setTimestamp(LocalDateTime.now());
                vndPrice.setChange24h(usdPrice.getChange24h());
                vndPrice.setChangePercent24h(usdPrice.getChangePercent24h());

                vndPrices.add(vndPrice);
            }

            return vndPrices;

        } catch (Exception e) {
            logger.error("Error converting prices to VND: {}", e.getMessage());
            throw new RuntimeException("Failed to get VND prices: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate material cost for a given weight and metal type
     */
    public MaterialCostResponse calculateMaterialCost(MaterialCostRequest request) {
        try {
            // Get current prices
            List<PreciousMetalPriceResponse> prices = request.getCurrency().equalsIgnoreCase("VND")
                ? getPricesInVND()
                : getPricesInUSD();

            // Find price for requested metal
            PreciousMetalPriceResponse metalPrice = prices.stream()
                .filter(p -> p.getMetalType().equalsIgnoreCase(request.getMetalType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Metal type not found: " + request.getMetalType()));

            // Convert weight to grams
            BigDecimal weightInGrams = convertToGrams(request.getWeight(), request.getWeightUnit());

            // Calculate cost per piece
            BigDecimal costPerPiece = weightInGrams.multiply(metalPrice.getPricePerGram())
                .setScale(2, RoundingMode.HALF_UP);

            // Calculate total cost
            BigDecimal totalCost = costPerPiece.multiply(BigDecimal.valueOf(request.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

            // Build response
            MaterialCostResponse response = new MaterialCostResponse();
            response.setMetalType(request.getMetalType());
            response.setWeightInGrams(weightInGrams);
            response.setPricePerGram(metalPrice.getPricePerGram());
            response.setCostPerPiece(costPerPiece);
            response.setTotalCost(totalCost);
            response.setQuantity(request.getQuantity());
            response.setCurrency(request.getCurrency());
            response.setCalculatedAt(LocalDateTime.now());
            response.setPriceSource(metalPrice.getSource());

            logger.info("Calculated material cost: {} {} of {} = {} {}",
                weightInGrams, "grams", request.getMetalType(), totalCost, request.getCurrency());

            return response;

        } catch (Exception e) {
            logger.error("Error calculating material cost: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate material cost: " + e.getMessage(), e);
        }
    }

    /**
     * Convert weight to grams
     */
    private BigDecimal convertToGrams(BigDecimal weight, String unit) {
        switch (unit.toUpperCase()) {
            case "GRAMS":
            case "GRAM":
            case "G":
                return weight;
            case "OUNCES":
            case "OUNCE":
            case "OZ":
                return weight.multiply(GRAMS_PER_OUNCE);
            case "LUONG":
                return weight.multiply(GRAMS_PER_LUONG);
            case "CHI":
                return weight.multiply(GRAMS_PER_CHI);
            default:
                throw new RuntimeException("Unknown weight unit: " + unit);
        }
    }

    /**
     * Get mock price for a specific metal
     */
    private PreciousMetalPriceResponse getMockPrice(String metalType) {
        PreciousMetalPriceResponse price = new PreciousMetalPriceResponse();
        price.setMetalType(metalType);
        price.setCurrency("USD");
        price.setSource("MOCK DATA (API unavailable)");
        price.setTimestamp(LocalDateTime.now());

        switch (metalType) {
            case "GOLD":
                price.setPricePerOunce(BigDecimal.valueOf(2650.00));
                price.setPricePerGram(BigDecimal.valueOf(85.20));
                price.setChange24h(BigDecimal.valueOf(15.50));
                price.setChangePercent24h(BigDecimal.valueOf(0.59));
                break;
            case "SILVER":
                price.setPricePerOunce(BigDecimal.valueOf(31.50));
                price.setPricePerGram(BigDecimal.valueOf(1.01));
                price.setChange24h(BigDecimal.valueOf(0.45));
                price.setChangePercent24h(BigDecimal.valueOf(1.45));
                break;
            case "PLATINUM":
                price.setPricePerOunce(BigDecimal.valueOf(980.00));
                price.setPricePerGram(BigDecimal.valueOf(31.51));
                price.setChange24h(BigDecimal.valueOf(-5.20));
                price.setChangePercent24h(BigDecimal.valueOf(-0.53));
                break;
            case "PALLADIUM":
                price.setPricePerOunce(BigDecimal.valueOf(950.00));
                price.setPricePerGram(BigDecimal.valueOf(30.54));
                price.setChange24h(BigDecimal.valueOf(8.75));
                price.setChangePercent24h(BigDecimal.valueOf(0.93));
                break;
            default:
                return null;
        }

        return price;
    }

    /**
     * MOCK DATA - For development or when all APIs fail
     */
    private List<PreciousMetalPriceResponse> getMockPricesUSD() {
        List<PreciousMetalPriceResponse> prices = new ArrayList<>();
        for (String metal : List.of("GOLD", "SILVER", "PLATINUM", "PALLADIUM")) {
            PreciousMetalPriceResponse price = getMockPrice(metal);
            if (price != null) {
                prices.add(price);
            }
        }
        return prices;
    }

    /**
     * Force refresh prices (bypass cache)
     */
    public List<PreciousMetalPriceResponse> refreshPrices() {
        logger.info("Force refreshing metal prices...");
        // This method doesn't use cache, so it will always fetch fresh data
        boolean originalMockSetting = useMockData;
        useMockData = false;

        List<PreciousMetalPriceResponse> prices = new ArrayList<>();
        for (String metal : List.of("GOLD", "SILVER", "PLATINUM", "PALLADIUM")) {
            PreciousMetalPriceResponse price = fetchFromGoldApi(metal);
            if (price == null) {
                price = fetchFromMetalsDev(metal);
            }
            if (price == null) {
                price = getMockPrice(metal);
            }
            if (price != null) {
                prices.add(price);
                lastKnownPrices.put(metal, price);
            }
        }

        useMockData = originalMockSetting;
        return prices;
    }
}
