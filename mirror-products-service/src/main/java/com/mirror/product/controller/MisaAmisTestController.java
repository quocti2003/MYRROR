package com.mirror.product.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mirror.product.service.misa.MisaAmisTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Test controller to exercise every MISA AMIS API operation.
 * All endpoints call MisaAmisApiClient directly with test payloads
 * and return the raw MISA response.
 *
 * Base path: /api/misa/amis/test
 */
@RestController
@RequestMapping("/api/misa/amis/test")
@RequiredArgsConstructor
@Slf4j
public class MisaAmisTestController {

    private final MisaAmisTestService testService;
    private final ObjectMapper objectMapper;

    /**
     * Create a test warehouse via saveDictionary(5, ...).
     *
     * Optional body: { "code": "KHO-TEST-001", "name": "Kho Test Mirror" }
     */
    @PostMapping("/warehouse")
    public ResponseEntity<JsonNode> createWarehouse(@RequestBody(required = false) Map<String, String> body) {
        String code = body != null ? body.get("code") : null;
        String name = body != null ? body.get("name") : null;
        String branchId = body != null ? body.get("branchId") : null;
        try {
            JsonNode result = testService.createWarehouse(code, name, branchId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test create warehouse failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Create a test inventory item via saveDictionary(3, ...).
     *
     * Optional body: { "code": "TEST-ITEM-001", "name": "Test Item", "branchId": "..." }
     */
    @PostMapping("/inventory-item")
    public ResponseEntity<JsonNode> createInventoryItem(@RequestBody(required = false) Map<String, String> body) {
        String code = body != null ? body.get("code") : null;
        String name = body != null ? body.get("name") : null;
        String branchId = body != null ? body.get("branchId") : null;
        try {
            JsonNode result = testService.createInventoryItem(code, name, branchId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test create inventory item failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Create a customer via saveDictionary(1, ...).
     *
     * Body: { "code": "KH001", "name": "Nguyen Van A", "branchId": "...",
     *         "phone": "0901234567", "email": "a@test.com", "address": "123 ABC",
     *         "provinceOrCity": "Ho Chi Minh", "country": "Việt Nam",
     *         "taxCode": "0123456789" }
     *
     * code, name, branchId are the main fields. All others are optional extras.
     */
    @PostMapping("/customer")
    public ResponseEntity<JsonNode> createCustomer(@RequestBody(required = false) Map<String, Object> body) {
        String code = getString(body, "code", null);
        String name = getString(body, "name", null);
        String branchId = getString(body, "branchId", null);

        // Collect extra fields beyond the core three
        Map<String, Object> extraFields = new java.util.HashMap<>();
        if (body != null) {
            if (body.containsKey("phone")) {
                extraFields.put("mobile", body.get("phone"));
                extraFields.put("tel", body.get("phone"));
            }
            if (body.containsKey("email")) extraFields.put("email_address", body.get("email"));
            if (body.containsKey("address")) extraFields.put("address", body.get("address"));
            if (body.containsKey("provinceOrCity")) extraFields.put("province_or_city", body.get("provinceOrCity"));
            if (body.containsKey("country")) extraFields.put("country", body.get("country"));
            if (body.containsKey("taxCode")) extraFields.put("company_tax_code", body.get("taxCode"));
        }

        try {
            JsonNode result = testService.createCustomer(code, name, branchId,
                    extraFields.isEmpty() ? null : extraFields);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Create customer failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Create a test account object group via saveDictionary(2, ...).
     */
    @PostMapping("/account-object-group")
    public ResponseEntity<JsonNode> createAccountObjectGroup(@RequestBody(required = false) Map<String, String> body) {
        String code = body != null ? body.get("code") : null;
        String name = body != null ? body.get("name") : null;
        String branchId = body != null ? body.get("branchId") : null;
        try {
            JsonNode result = testService.createAccountObjectGroup(code, name, branchId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test create account object group failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Create a test inventory category via saveDictionary(4, ...).
     */
    @PostMapping("/inventory-category")
    public ResponseEntity<JsonNode> createInventoryCategory(@RequestBody(required = false) Map<String, String> body) {
        String code = body != null ? body.get("code") : null;
        String name = body != null ? body.get("name") : null;
        String branchId = body != null ? body.get("branchId") : null;
        try {
            JsonNode result = testService.createInventoryCategory(code, name, branchId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test create inventory category failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Create a test unit of measurement via saveDictionary(6, ...).
     */
    @PostMapping("/unit")
    public ResponseEntity<JsonNode> createUnit(@RequestBody(required = false) Map<String, String> body) {
        String name = body != null ? body.get("name") : null;
        String branchId = body != null ? body.get("branchId") : null;
        try {
            JsonNode result = testService.createUnit(name, branchId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test create unit failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Create a stock-in (nhap kho) voucher via saveVoucher(7, ...).
     *
     * Optional body: { "itemCode", "itemId", "stockCode", "stockId", "quantity", "unitPrice", "accountObjectCode" }
     * Defaults: stockCode=S00001, quantity=1, unitPrice=100000
     */
    @PostMapping("/stock-in")
    public ResponseEntity<JsonNode> createStockIn(@RequestBody(required = false) Map<String, Object> body) {
        try {
            String itemCode = getString(body, "itemCode", "TEST-ITEM");
            String itemId = getString(body, "itemId", null);
            String stockCode = getString(body, "stockCode", "S00001");
            String stockId = getString(body, "stockId", null);
            int quantity = getInt(body, "quantity", 1);
            long unitPrice = getLong(body, "unitPrice", 100000);
            String accountObjectCode = getString(body, "accountObjectCode", null);

            JsonNode result = testService.createStockIn(itemCode, itemId, stockCode, stockId, quantity, unitPrice, accountObjectCode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test stock-in failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Create a stock-out (xuat kho) voucher via saveVoucher(8, ...).
     *
     * Optional body: { "itemCode", "itemId", "stockCode", "stockId", "quantity", "unitPrice" }
     */
    @PostMapping("/stock-out")
    public ResponseEntity<JsonNode> createStockOut(@RequestBody(required = false) Map<String, Object> body) {
        try {
            String itemCode = getString(body, "itemCode", "TEST-ITEM");
            String itemId = getString(body, "itemId", null);
            String stockCode = getString(body, "stockCode", null);
            String stockId = getString(body, "stockId", null);
            int quantity = getInt(body, "quantity", 5);
            long unitPrice = getLong(body, "unitPrice", 100000);

            JsonNode result = testService.createStockOut(itemCode, itemId, stockCode, stockId, quantity, unitPrice);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test stock-out failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Create a stock transfer voucher via saveVoucher(9, ...).
     *
     * Optional body: { "itemCode", "itemId", "fromStockCode", "fromStockId", "toStockCode", "toStockId", "quantity", "unitPrice" }
     */
    @PostMapping("/stock-transfer")
    public ResponseEntity<JsonNode> createStockTransfer(@RequestBody(required = false) Map<String, Object> body) {
        try {
            String itemCode = getString(body, "itemCode", "TEST-ITEM");
            String itemId = getString(body, "itemId", null);
            String fromStockCode = getString(body, "fromStockCode", null);
            String fromStockId = getString(body, "fromStockId", null);
            String toStockCode = getString(body, "toStockCode", null);
            String toStockId = getString(body, "toStockId", null);
            int quantity = getInt(body, "quantity", 5);
            long unitPrice = getLong(body, "unitPrice", 100000);

            JsonNode result = testService.createStockTransfer(itemCode, itemId,
                    fromStockCode, fromStockId, toStockCode, toStockId, quantity, unitPrice);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test stock transfer failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Create a sales voucher via saveVoucher(13, ...).
     *
     * Optional body: { "itemCode", "itemId", "stockCode", "stockId", "customerId", "customerCode",
     *                   "quantity", "unitPrice", "costPrice" }
     */
    @PostMapping("/sales")
    public ResponseEntity<JsonNode> createSalesVoucher(@RequestBody(required = false) Map<String, Object> body) {
        try {
            String itemCode = getString(body, "itemCode", "TEST-ITEM");
            String itemId = getString(body, "itemId", null);
            String stockCode = getString(body, "stockCode", null);
            String stockId = getString(body, "stockId", null);
            String customerId = getString(body, "customerId", null);
            String customerCode = getString(body, "customerCode", null);
            int quantity = getInt(body, "quantity", 2);
            long unitPrice = getLong(body, "unitPrice", 200000);
            long costPrice = getLong(body, "costPrice", 100000);

            JsonNode result = testService.createSalesVoucher(itemCode, itemId, stockCode, stockId,
                    customerId, customerCode, quantity, unitPrice, costPrice);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test sales voucher failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Read inventory balance from MISA AMIS.
     */
    @GetMapping("/balance")
    public ResponseEntity<JsonNode> getBalance(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "100") int take,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String stockId) {
        try {
            JsonNode result = testService.getBalance(skip, take, branchId, stockId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test get balance failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Poll callback results from MISA AMIS.
     */
    @GetMapping("/poll")
    public ResponseEntity<JsonNode> pollCallbackResults(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "100") int take) {
        try {
            JsonNode result = testService.pollCallbackResults(fromDate, toDate, skip, take);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test poll callback failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Read dictionary data from MISA AMIS by data_type.
     * Useful for discovering the correct data_type mapping for get_dictionary endpoint.
     */
    @GetMapping("/dictionary/{dataType}")
    public ResponseEntity<JsonNode> readDictionary(
            @PathVariable int dataType,
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "100") int take) {
        try {
            JsonNode result = testService.readDictionary(dataType, skip, take);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test read dictionary type {} failed", dataType, e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    /**
     * Run complete lifecycle test: authenticate -> create item -> stock in -> balance ->
     * sales -> balance -> summary.
     */
    @PostMapping("/full-lifecycle")
    public ResponseEntity<JsonNode> runFullLifecycleTest() {
        log.info("Starting full MISA AMIS lifecycle test");
        try {
            ObjectNode result = testService.runFullLifecycleTest();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Full lifecycle test failed", e);
            return ResponseEntity.internalServerError().body(errorNode(e));
        }
    }

    // ==================== HELPERS ====================

    private JsonNode errorNode(Exception e) {
        return objectMapper.createObjectNode()
                .put("error", e.getMessage())
                .put("type", e.getClass().getSimpleName());
    }

    private String getString(Map<String, Object> body, String key, String defaultValue) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) return defaultValue;
        return String.valueOf(body.get(key));
    }

    private int getInt(Map<String, Object> body, String key, int defaultValue) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) return defaultValue;
        Object val = body.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(String.valueOf(val));
    }

    private long getLong(Map<String, Object> body, String key, long defaultValue) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) return defaultValue;
        Object val = body.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(String.valueOf(val));
    }
}
