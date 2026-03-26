package com.mirror.product.client.misa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.config.misa.MisaConfig.MisaAmisApiProperties;
import com.mirror.product.enums.misa.MisaDictionaryType.GetDictionaryType;
import com.mirror.product.enums.misa.MisaDictionaryType.SaveDictionaryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for MISA AMIS ACT Open API
 *
 * This is separate from MisaApiClient which handles the MISA eShop API.
 * AMIS uses different authentication (X-MISA-AccessToken header) and endpoints.
 *
 * Base URL: https://actapp.misa.vn
 * Documentation: https://actdocs.misa.vn
 */
@Component
@Slf4j
public class MisaAmisApiClient {

    private static final String ACCESS_TOKEN_HEADER = "X-MISA-AccessToken";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final MisaAmisApiProperties amisProperties;

    private String accessToken;
    private LocalDateTime tokenExpiryTime;

    public MisaAmisApiClient(WebClient.Builder webClientBuilder,
                              ObjectMapper objectMapper,
                              MisaAmisApiProperties amisProperties) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.amisProperties = amisProperties;
    }

    public MisaAmisApiProperties getAmisProperties() {
        return amisProperties;
    }

    /**
     * Get access token from MISA AMIS API
     * Uses the app_id, access_code, and org_company_code for authentication
     * Endpoint: POST /api/oauth/actopen/connect
     */
    public Mono<Boolean> authenticate() {
        log.info("Authenticating with MISA AMIS API - AppID: {}, OrgCode: {}",
                amisProperties.getAppId(), amisProperties.getOrgCompanyCode());

        String url = amisProperties.getBaseUrl() + "/api/oauth/actopen/connect";

        Map<String, String> payload = new HashMap<>();
        payload.put("app_id", amisProperties.getAppId());
        payload.put("access_code", amisProperties.getAccessCode());
        payload.put("org_company_code", amisProperties.getOrgCompanyCode());

        log.debug("AMIS Auth URL: {}", url);
        log.debug("AMIS Auth Payload: app_id={}, org_company_code={}",
                amisProperties.getAppId(), amisProperties.getOrgCompanyCode());

        return webClient.post()
            .uri(url)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> {
                log.debug("AMIS Auth Response: {}", response);

                // Check if success
                boolean success = response.has("Success") && response.get("Success").asBoolean();
                if (!success) {
                    String errorMessage = response.has("ErrorMessage")
                        ? response.get("ErrorMessage").asText()
                        : "Unknown error";
                    log.error("MISA AMIS authentication failed: {}", errorMessage);
                    return false;
                }

                // Parse the Data field (it's a JSON string containing the access_token)
                if (response.has("Data")) {
                    try {
                        String dataJson = response.get("Data").asText();
                        JsonNode dataNode = objectMapper.readTree(dataJson);

                        if (dataNode.has("access_token")) {
                            this.accessToken = dataNode.get("access_token").asText();
                            // Token expires in 12 hours, refresh at 11 hours
                            this.tokenExpiryTime = LocalDateTime.now().plusHours(11);
                            String appName = dataNode.has("app_name") ? dataNode.get("app_name").asText() : "Unknown";
                            log.info("MISA AMIS authentication successful for: {}", appName);
                            return true;
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse AMIS Data field: {}", e.getMessage());
                    }
                }

                log.error("MISA AMIS authentication failed: No access_token in response");
                return false;
            })
            .doOnError(error -> log.error("MISA AMIS authentication error: {}", error.getMessage()))
            .onErrorReturn(false);
    }

    /**
     * Ensure we have a valid access token
     */
    private Mono<Boolean> ensureAuthenticated() {
        if (accessToken == null || tokenExpiryTime == null || LocalDateTime.now().isAfter(tokenExpiryTime)) {
            log.info("AMIS access token expired or missing, re-authenticating...");
            return authenticate();
        }
        return Mono.just(true);
    }

    /**
     * Get dictionary data from MISA AMIS
     * Endpoint: POST /apir/sync/actopen/get_dictionary
     *
     * @param dataType Data type: 1=accounts, 2=inventory items, 5=warehouses, 4=units
     * @param skip Number of records to skip (pagination)
     * @param take Number of records to take (max 100)
     * @return Dictionary data response
     */
    public Mono<JsonNode> getDictionaryData(int dataType, int skip, int take) {
        return ensureAuthenticated()
            .flatMap(authenticated -> {
                if (!authenticated) {
                    return Mono.error(new RuntimeException("AMIS authentication failed"));
                }

                String url = amisProperties.getBaseUrl() + "/apir/sync/actopen/get_dictionary";

                Map<String, Object> payload = new HashMap<>();
                payload.put("app_id", amisProperties.getAppId());
                payload.put("org_company_code", amisProperties.getOrgCompanyCode());
                payload.put("data_type", dataType);
                payload.put("skip", skip);
                payload.put("take", Math.min(take, 100));

                log.info("Fetching AMIS dictionary data - Type: {}, Skip: {}, Take: {}",
                        dataType, skip, take);

                return webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(ACCESS_TOKEN_HEADER, accessToken)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .doOnSuccess(response -> log.debug("AMIS dictionary response: {}", response))
                    .doOnError(error -> log.error("Error fetching AMIS dictionary data", error));
            });
    }

    /**
     * Get customers/objects (GetDictionaryType.ACCOUNT_OBJECT = 1)
     */
    public Mono<JsonNode> getAccounts(int skip, int take) {
        return getDictionaryData(GetDictionaryType.ACCOUNT_OBJECT.getValue(), skip, take);
    }

    /**
     * Get inventory items (GetDictionaryType.INVENTORY_ITEM = 2)
     */
    public Mono<JsonNode> getInventoryItems(int skip, int take) {
        return getDictionaryData(GetDictionaryType.INVENTORY_ITEM.getValue(), skip, take);
    }

    /**
     * Get warehouses (GetDictionaryType.STOCK = 3)
     */
    public Mono<JsonNode> getWarehouses(int skip, int take) {
        return getDictionaryData(GetDictionaryType.STOCK.getValue(), skip, take);
    }

    /**
     * Get units of measure (GetDictionaryType.UNIT = 4)
     */
    public Mono<JsonNode> getUnits(int skip, int take) {
        return getDictionaryData(GetDictionaryType.UNIT.getValue(), skip, take);
    }

    /**
     * Get inventory balance from MISA AMIS
     * Endpoint: POST /apir/sync/actopen/get_list_inventory_balance
     * Note: Vouchers must be posted (ghi so) in MISA for balance to appear
     *
     * @param skip Records to skip (pagination)
     * @param take Records to take (max 100)
     * @param branchId Optional branch ID filter
     * @param stockId Optional warehouse/stock ID filter
     * @return Inventory balance data
     */
    public Mono<JsonNode> getInventoryBalance(int skip, int take, String branchId, String stockId) {
        return ensureAuthenticated()
            .flatMap(authenticated -> {
                if (!authenticated) {
                    return Mono.error(new RuntimeException("AMIS authentication failed"));
                }

                String url = amisProperties.getBaseUrl() + "/apir/sync/actopen/get_list_inventory_balance";

                Map<String, Object> payload = new HashMap<>();
                payload.put("app_id", amisProperties.getAppId());
                payload.put("org_company_code", amisProperties.getOrgCompanyCode());
                payload.put("skip", skip);
                payload.put("take", Math.min(take, 100));

                if (branchId != null && !branchId.isEmpty()) {
                    payload.put("branch_id", branchId);
                }
                if (stockId != null && !stockId.isEmpty()) {
                    payload.put("stock_id", stockId);
                }

                log.info("Fetching AMIS inventory balance - Skip: {}, Take: {}, StockId: {}", skip, take, stockId);

                return webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(ACCESS_TOKEN_HEADER, accessToken)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .doOnSuccess(response -> log.debug("AMIS inventory balance response: {}", response))
                    .doOnError(error -> log.error("Error fetching AMIS inventory balance", error));
            });
    }

    /**
     * Save voucher to MISA AMIS
     * Endpoint: POST /apir/sync/actopen/save
     * (Note: NOT /save_voucher - the correct endpoint is just /save)
     *
     * @param voucherType Voucher type (e.g., 501=Warehouse Import, 502=Warehouse Export)
     * @param voucherData Voucher data object
     * @return Response from MISA
     */
    public Mono<JsonNode> saveVoucher(int voucherType, Object voucherData) {
        return ensureAuthenticated()
            .flatMap(authenticated -> {
                if (!authenticated) {
                    return Mono.error(new RuntimeException("AMIS authentication failed"));
                }

                String url = amisProperties.getBaseUrl() + "/apir/sync/actopen/save";

                // Ensure voucherData is a list (API requires array of vouchers)
                List<Object> voucherList;
                if (voucherData instanceof List) {
                    voucherList = (List<Object>) voucherData;
                } else {
                    // Single voucher - wrap in list
                    if (voucherData instanceof Map) {
                        Map<String, Object> voucherMap = (Map<String, Object>) voucherData;
                        // Ensure voucher_type is inside the voucher object
                        if (!voucherMap.containsKey("voucher_type")) {
                            voucherMap.put("voucher_type", voucherType);
                        }
                    }
                    voucherList = List.of(voucherData);
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("app_id", amisProperties.getAppId());
                payload.put("org_company_code", amisProperties.getOrgCompanyCode());
                payload.put("voucher", voucherList);

                if (amisProperties.getCallbackUrl() != null && !amisProperties.getCallbackUrl().isEmpty()) {
                    payload.put("callback_url", amisProperties.getCallbackUrl());
                }

                log.info("Saving voucher to AMIS - Type: {}, Count: {}", voucherType, voucherList.size());

                return webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(ACCESS_TOKEN_HEADER, accessToken)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .doOnSuccess(response -> log.info("AMIS save voucher response: {}", response))
                    .doOnError(error -> log.error("Error saving voucher to AMIS", error));
            });
    }

    /**
     * Save dictionary data to MISA AMIS
     * Endpoint: POST /apir/sync/actopen/save_dictionary
     *
     * Payload structure (per BeeHexa/MISA docs):
     * {
     *   "org_company_code": "mirror",
     *   "app_id": "...",
     *   "dictionary": [
     *     { "dictionary_type": 3, "inventory_item_code": "...", ... }
     *   ]
     * }
     *
     * Note: dictionary_type goes INSIDE each dictionary item, not at the top level.
     * The array key is "dictionary", not "data".
     *
     * Dictionary type mapping (per actdocs.misa.vn):
     *   1  = Đối tượng (account_object) — customers/vendors/employees
     *   2  = Nhóm đối tượng (account_object_group)
     *   3  = Vật tư (inventory_item) — inventory items/products
     *   4  = Nhóm vật tư (inventory_item_category)
     *   5  = Kho (stock) — warehouses
     *   6  = Đơn vị tính (unit) — units of measurement
     *   7  = Tài khoản ngân hàng (bank_account)
     *   8  = Ngân hàng (bank)
     *   9  = Khoản mục chi phí (expense_item)
     *   10 = Mục thu chi (budget_item)
     *   12 = Đối tượng THCP (job/cost object)
     *
     * Field naming per type:
     *   Type 1: account_object_code, account_object_name, account_object_id
     *   Type 3: inventory_item_code, inventory_item_name, inventory_item_id
     *   Type 5: stock_code, stock_name, stock_id
     *   Most types require branch_id.
     *
     * @param dictionaryType Dictionary type (1=Customer, 3=Inventory Item, 5=Warehouse, etc.)
     * @param data List of dictionary items (each should already contain dictionary_type)
     * @return Response from MISA
     */
    public Mono<JsonNode> saveDictionary(int dictionaryType, List<Object> data) {
        return ensureAuthenticated()
            .flatMap(authenticated -> {
                if (!authenticated) {
                    return Mono.error(new RuntimeException("AMIS authentication failed"));
                }

                String url = amisProperties.getBaseUrl() + "/apir/sync/actopen/save_dictionary";

                // Inject dictionary_type into each item if not already present
                List<Object> enrichedData = new java.util.ArrayList<>();
                for (Object item : data) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) item;
                        if (!map.containsKey("dictionary_type")) {
                            map.put("dictionary_type", dictionaryType);
                        }
                        enrichedData.add(map);
                    } else {
                        enrichedData.add(item);
                    }
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("org_company_code", amisProperties.getOrgCompanyCode());
                payload.put("app_id", amisProperties.getAppId());
                payload.put("dictionary", enrichedData);

                if (amisProperties.getCallbackUrl() != null && !amisProperties.getCallbackUrl().isEmpty()) {
                    payload.put("callback_url", amisProperties.getCallbackUrl());
                }

                log.info("Saving dictionary to AMIS - Type: {}, Count: {}, Payload: {}", dictionaryType, data.size(), payload);

                return webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(ACCESS_TOKEN_HEADER, accessToken)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .doOnSuccess(response -> log.info("AMIS save dictionary response: {}", response))
                    .doOnError(error -> log.error("Error saving dictionary to AMIS", error));
            });
    }

    /**
     * Get callback detail/error results from MISA AMIS
     * This allows polling for results instead of relying on webhook callbacks.
     * Endpoint: POST /apir/sync/actopen/get_call_back_detail_error
     *
     * @param fromDate Start date (yyyy-MM-dd format)
     * @param toDate End date (yyyy-MM-dd format)
     * @param skip Records to skip (pagination)
     * @param take Records to take (max 100)
     * @return Response with callback results
     */
    public Mono<JsonNode> getCallbackDetailError(String fromDate, String toDate, int skip, int take) {
        return ensureAuthenticated()
            .flatMap(authenticated -> {
                if (!authenticated) {
                    return Mono.error(new RuntimeException("AMIS authentication failed"));
                }

                String url = amisProperties.getBaseUrl() + "/apir/sync/actopen/get_call_back_detail_error";

                Map<String, Object> payload = new HashMap<>();
                payload.put("app_id", amisProperties.getAppId());
                payload.put("org_company_code", amisProperties.getOrgCompanyCode());
                payload.put("from_date", fromDate);
                payload.put("to_date", toDate);
                payload.put("skip", skip);
                payload.put("take", Math.min(take, 100));

                log.info("Fetching callback detail/error from AMIS - from: {}, to: {}, skip: {}, take: {}",
                        fromDate, toDate, skip, take);

                return webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(ACCESS_TOKEN_HEADER, accessToken)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .doOnSuccess(response -> log.info("AMIS callback detail response: {}", response))
                    .doOnError(error -> log.error("Error fetching callback detail from AMIS", error));
            });
    }

    // Getters for authentication state
    public String getAccessToken() {
        return accessToken;
    }

    public boolean isAuthenticated() {
        return accessToken != null && tokenExpiryTime != null && LocalDateTime.now().isBefore(tokenExpiryTime);
    }

    public MisaAmisApiProperties getProperties() {
        return amisProperties;
    }
}
