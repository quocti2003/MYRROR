package com.mirror.product.client.misa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.dto.notification.MisaApiResponse;
import com.mirror.product.dto.notification.MisaAuthResponse;
import com.mirror.product.dto.notification.MisaCustomerDto;
import com.mirror.product.dto.notification.MisaInventoryItemDto;
import com.mirror.product.dto.notification.MisaInvoiceDto;
import com.mirror.product.dto.notification.MisaProductCategoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Component
@Slf4j
public class MisaApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${misa.api.domain}")
    private String domain;

    @Value("${misa.api.app-id}")
    private String appId;

    @Value("${misa.api.secret-key}")
    private String secretKey;

    @Value("${misa.api.base-url:https://graphapi.mshopkeeper.vn}")
    private String baseUrl;

    private String accessToken;
    private String companyCode;
    private String environment;
    private LocalDateTime tokenExpiryTime;

    public MisaApiClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Authenticate with MISA API using the working signature method (JSON Sorted)
     */
    public Mono<Boolean> authenticate() {
        log.info("Authenticating with MISA API - Domain: {}, AppID: {}", domain, appId);

        try {
            String loginTime = LocalDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"));

            // Generate signature using JSON Sorted method (the working method)
            String signature = generateSignature(loginTime);

            Map<String, String> authPayload = new HashMap<>();
            authPayload.put("Domain", domain);
            authPayload.put("AppID", appId);
            authPayload.put("LoginTime", loginTime);
            authPayload.put("SignatureInfo", signature);

            return webClient.post()
                .uri(baseUrl + "/auth/api/Account/Login")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(authPayload)
                .retrieve()
                .bodyToMono(MisaAuthResponse.class)
                .map(response -> {
                    if (response.getSuccess() != null && response.getSuccess()) {
                        MisaAuthResponse.AuthData authData = response.getData();
                        this.accessToken = authData.getAccessToken();
                        this.companyCode = authData.getCompanyCode();
                        this.environment = authData.getEnvironment();
                        this.tokenExpiryTime = LocalDateTime.now().plusHours(1); // Assume 1 hour expiry

                        log.info("MISA authentication successful - Environment: {}, Company: {}",
                                environment, companyCode);
                        return true;
                    } else {
                        log.error("MISA authentication failed: {}", response.getErrorMessage());
                        return false;
                    }
                })
                .doOnError(error -> log.error("MISA authentication error", error));

        } catch (Exception e) {
            log.error("Error during MISA authentication", e);
            return Mono.just(false);
        }
    }

    /**
     * Generate HMAC-SHA256 signature using JSON Sorted method
     */
    private String generateSignature(String loginTime) throws Exception {
        // Create JSON with sorted keys using TreeMap for automatic sorting
        Map<String, String> signatureData = new TreeMap<>();
        signatureData.put("Domain", domain);
        signatureData.put("AppID", appId);
        signatureData.put("LoginTime", loginTime);

        // Convert to JSON string with sorted keys
        String jsonString = objectMapper.writeValueAsString(signatureData);

        // Generate HMAC-SHA256 signature
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);

        byte[] hash = mac.doFinal(jsonString.getBytes(StandardCharsets.UTF_8));

        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Check if authentication is valid and refresh if needed
     */
    private Mono<Boolean> ensureAuthenticated() {
        if (accessToken == null || tokenExpiryTime == null || LocalDateTime.now().isAfter(tokenExpiryTime)) {
            log.info("Access token expired or missing, re-authenticating...");
            return authenticate();
        }
        return Mono.just(true);
    }

    /**
     * Fetch inventory items from MISA API
     */
    public Mono<MisaApiResponse<MisaInventoryItemDto>> getInventoryItems(
            int page,
            int limit,
            String sortField,
            String sortType,
            boolean includeInventory,
            String categoryId,
            boolean includeInactive) {

        return ensureAuthenticated()
            .flatMap(authenticated -> {
                if (!authenticated) {
                    return Mono.error(new RuntimeException("Authentication failed"));
                }

                String url = String.format("%s/%s/api/v1/inventoryitems/pagingwithdetail", baseUrl, environment);

                Map<String, Object> payload = new HashMap<>();
                payload.put("Page", page);
                payload.put("Limit", Math.min(limit, 100)); // Max 100 as per API docs
                payload.put("SortField", sortField);
                payload.put("SortType", sortType);
                payload.put("IncludeInventory", includeInventory);
                payload.put("IncludeInActive", includeInactive);

                if (categoryId != null) {
                    payload.put("InventoryItemCategoryID", categoryId);
                }

                log.info("Fetching inventory items from MISA - Page: {}, Limit: {}", page, limit);

                return webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("CompanyCode", companyCode)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(jsonNode -> {
                        try {
                            return objectMapper.convertValue(jsonNode, new TypeReference<MisaApiResponse<MisaInventoryItemDto>>() {});
                        } catch (Exception e) {
                            log.error("Error parsing MISA inventory response", e);
                            throw new RuntimeException("Failed to parse MISA response", e);
                        }
                    })
                    .doOnSuccess(response -> {
                        if (response.getSuccess() != null && response.getSuccess()) {
                            log.info("Successfully fetched {} inventory items from MISA",
                                    response.getData() != null ? response.getData().size() : 0);
                        } else {
                            log.warn("MISA inventory fetch returned error: {}", response.getErrorMessage());
                        }
                    })
                    .doOnError(error -> log.error("Error fetching inventory items from MISA", error));
            });
    }

    /**
     * Fetch all inventory items by paginating through all pages
     */
    public Mono<MisaApiResponse<MisaInventoryItemDto>> getAllInventoryItems(int maxItems) {
        // Implementation for fetching all pages would go here
        // This is a simplified version that fetches the first page
        return getInventoryItems(1, Math.min(maxItems, 100), "Code", "1", true, null, true);
    }

    /**
     * Fetch product categories from MISA API
     */
    public Mono<MisaApiResponse<MisaProductCategoryDto>> getProductCategories(boolean includeInactive) {
        return ensureAuthenticated()
            .flatMap(authenticated -> {
                if (!authenticated) {
                    return Mono.error(new RuntimeException("Authentication failed"));
                }

                String url = String.format("%s/%s/api/v1/categories/list", baseUrl, environment);

                log.info("Fetching product categories from MISA - Include Inactive: {}", includeInactive);

                return webClient.get()
                    .uri(url + "?includeInactive=" + includeInactive)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("CompanyCode", companyCode)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(jsonNode -> {
                        try {
                            return objectMapper.convertValue(jsonNode, new TypeReference<MisaApiResponse<MisaProductCategoryDto>>() {});
                        } catch (Exception e) {
                            log.error("Error parsing MISA categories response", e);
                            throw new RuntimeException("Failed to parse MISA categories response", e);
                        }
                    })
                    .doOnSuccess(response -> {
                        if (response.getSuccess() != null && response.getSuccess()) {
                            log.info("Successfully fetched {} product categories from MISA",
                                    response.getData() != null ? response.getData().size() : 0);
                        } else {
                            log.warn("MISA categories fetch returned error: {}", response.getErrorMessage());
                        }
                    })
                    .doOnError(error -> log.error("Error fetching product categories from MISA", error));
            });
    }

    // Getters for current authentication state
    public String getAccessToken() {
        return accessToken;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getEnvironment() {
        return environment;
    }

    public boolean isAuthenticated() {
        return accessToken != null && tokenExpiryTime != null && LocalDateTime.now().isBefore(tokenExpiryTime);
    }

    /**
     * Fetch customers from MISA API with pagination
     */
    public Mono<MisaApiResponse<MisaCustomerDto>> getCustomers(
            int page,
            int limit,
            String sortField,
            int sortType,
            String lastSyncDate) {

        return ensureAuthenticated()
            .flatMap(authenticated -> {
                if (!authenticated) {
                    return Mono.error(new RuntimeException("Authentication failed"));
                }

                String url = String.format("%s/%s/api/v1/customers/paging", baseUrl, environment);

                Map<String, Object> payload = new HashMap<>();
                payload.put("Page", page);
                payload.put("Limit", Math.min(limit, 100)); // Max 100 as per API docs
                payload.put("SortField", sortField != null ? sortField : "Name");
                payload.put("SortType", sortType);

                if (lastSyncDate != null) {
                    payload.put("LastSyncDate", lastSyncDate);
                }

                log.info("Fetching customers from MISA - Page: {}, Limit: {}", page, limit);

                return webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("CompanyCode", companyCode)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(jsonNode -> {
                        try {
                            return objectMapper.convertValue(jsonNode, new TypeReference<MisaApiResponse<MisaCustomerDto>>() {});
                        } catch (Exception e) {
                            log.error("Error parsing MISA customers response", e);
                            throw new RuntimeException("Failed to parse MISA customers response", e);
                        }
                    })
                    .doOnSuccess(response -> {
                        if (response.getSuccess() != null && response.getSuccess()) {
                            log.info("Successfully fetched {} customers from MISA",
                                    response.getData() != null ? response.getData().size() : 0);
                        } else {
                            log.warn("MISA customers fetch returned error: {}", response.getErrorMessage());
                        }
                    })
                    .doOnError(error -> log.error("Error fetching customers from MISA", error));
            });
    }

    /**
     * Fetch invoices from MISA API with pagination
     */
    public Mono<MisaApiResponse<MisaInvoiceDto>> getInvoices(
            int page,
            int limit,
            String customerId,
            String branchId,
            String fromDate,
            String toDate) {

        return ensureAuthenticated()
            .flatMap(authenticated -> {
                if (!authenticated) {
                    return Mono.error(new RuntimeException("Authentication failed"));
                }

                String url = String.format("%s/%s/api/v1/invoices/pagingbycustomer", baseUrl, environment);

                Map<String, Object> payload = new HashMap<>();
                payload.put("Page", page);
                payload.put("Limit", Math.min(limit, 100)); // Max 100 as per API docs

                if (customerId != null) {
                    payload.put("CustomerId", customerId);
                }
                if (branchId != null) {
                    payload.put("BranchId", branchId);
                }
                if (fromDate != null) {
                    payload.put("FromDate", fromDate);
                    payload.put("DateRangeType", 1); // 1 = invoice date
                }
                if (toDate != null) {
                    payload.put("ToDate", toDate);
                }

                log.info("Fetching invoices from MISA - Page: {}, Limit: {}, Date range: {} to {}", page, limit, fromDate, toDate);

                return webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("CompanyCode", companyCode)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(jsonNode -> {
                        try {
                            // Log the raw response for debugging
                            log.info("Raw MISA invoices response: {}", jsonNode.toPrettyString());
                            return objectMapper.convertValue(jsonNode, new TypeReference<MisaApiResponse<MisaInvoiceDto>>() {});
                        } catch (Exception e) {
                            log.error("Error parsing MISA invoices response", e);
                            throw new RuntimeException("Failed to parse MISA invoices response", e);
                        }
                    })
                    .doOnSuccess(response -> {
                        if (response.getSuccess() != null && response.getSuccess()) {
                            log.info("Successfully fetched {} invoices from MISA",
                                    response.getData() != null ? response.getData().size() : 0);
                        } else {
                            log.warn("MISA invoices fetch returned error: {}", response.getErrorMessage());
                        }
                    })
                    .doOnError(error -> log.error("Error fetching invoices from MISA", error));
            });
    }
}
