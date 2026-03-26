package com.mirror.product.service.misa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mirror.product.client.misa.MisaAmisApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test service for exercising all MISA AMIS API operations directly.
 * Builds test payloads and returns raw MISA responses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MisaAmisTestService {

    private static final DateTimeFormatter MISA_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final MisaAmisApiClient amisApiClient;
    private final ObjectMapper objectMapper;

    // ==================== DICTIONARY OPERATIONS ====================
    //
    // Dictionary type mapping (per actdocs.misa.vn #2-14):
    //   1  = Đối tượng (account_object)        → account_object_code, account_object_name
    //   2  = Nhóm đối tượng (account_object_group) → account_object_group_code, account_object_group_name
    //   3  = Vật tư (inventory_item)            → inventory_item_code, inventory_item_name
    //   4  = Nhóm vật tư (inventory_item_category) → inventory_category_code, inventory_category_name
    //   5  = Kho (stock)                        → stock_code, stock_name
    //   6  = Đơn vị tính (unit)                 → unit_id, unit_name
    //   7  = Tài khoản ngân hàng (bank_account)
    //   8  = Ngân hàng (bank)
    //   9  = Khoản mục chi phí (expense_item)
    //   10 = Mục thu chi (budget_item)
    //   12 = Đối tượng THCP (job/cost object)
    //
    // All types require branch_id. Array key is "dictionary", not "data".

    /**
     * Create a test warehouse via saveDictionary(5, data).
     * dictionary_type=5 for warehouse (kho).
     */
    public JsonNode createWarehouse(String code, String name, String branchId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        if (code == null || code.isEmpty()) code = "KHO-TEST-" + timestamp;
        if (name == null || name.isEmpty()) name = "Kho Test Mirror " + timestamp;

        String now = LocalDateTime.now().format(MISA_DATE_FORMAT);

        Map<String, Object> warehouse = new HashMap<>();
        warehouse.put("dictionary_type", 5);
        warehouse.put("stock_id", java.util.UUID.randomUUID().toString());
        warehouse.put("org_refid", "TEST-WH-" + timestamp);
        warehouse.put("stock_code", code);
        warehouse.put("stock_name", name);
        warehouse.put("inactive", false);
        warehouse.put("reftype_category", 0);
        warehouse.put("reftype", 0);
        warehouse.put("excel_row_index", 0);
        warehouse.put("is_valid", false);
        warehouse.put("auto_refno", false);
        warehouse.put("state", 0);
        warehouse.put("created_date", now);
        warehouse.put("modified_date", now);
        if (branchId != null && !branchId.isEmpty()) {
            warehouse.put("branch_id", branchId);
        }

        log.info("Creating test warehouse: code={}, name={}, branchId={}", code, name, branchId);
        return amisApiClient.saveDictionary(5, List.of(warehouse)).block();
    }

    /**
     * Create a test inventory item via saveDictionary(3, data).
     * Uses dictionary_type=3 per MISA AMIS ACT Open API docs.
     *
     * Excel template column → API field mapping:
     *   Mã hàng (*)              → inventory_item_code      (required)
     *   Tên hàng (*)             → inventory_item_name      (required)
     *   Tính chất                → inventory_item_type      (0=Hàng hóa, 1=NVL, 2=Thành phẩm, 3=Dịch vụ, 5=Combo)
     *   Đơn vị tính chính        → unit_name
     *   Mã nhóm VTHH             → inventory_item_category_code_list
     *   Thuế suất GTGT (%)       → tax_rate
     *   Số lượng tồn tối thiểu   → minimum_stock
     *   Nguồn gốc                → origin
     *   Mô tả                    → description
     *   TK kho                   → inventory_account        (e.g. "1561")
     *   TK doanh thu             → revenue_account          (e.g. "5111")
     *   TK chi phí               → cogs_account             (e.g. "632")
     *   Đơn giá mua cố định      → unit_price
     *   Đơn giá bán              → sale_price1
     *   Đơn giá bán 2            → sale_price2
     *   Đơn giá bán 3            → sale_price3
     *   Đơn giá bán cố định      → fixed_sale_price
     *   Là đơn giá bán sau thuế  → is_unit_price_after_tax
     *   Tỷ lệ CK mua hàng (%)   → purchase_discount_rate
     *   branch_id                → branch_id                (required)
     */
    public JsonNode createInventoryItem(String code, String name, String branchId) {
        return createInventoryItem(code, name, branchId, null);
    }

    public JsonNode createInventoryItem(String code, String name, String branchId,
                                         Map<String, Object> extraFields) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        if (code == null || code.isEmpty()) code = "TEST-ITEM-" + timestamp;
        if (name == null || name.isEmpty()) name = "Test Item Mirror " + timestamp;

        // Generate a UUID for the item (BeeHexa example provides inventory_item_id)
        String itemId = java.util.UUID.randomUUID().toString();

        Map<String, Object> item = new HashMap<>();
        // === Core identity (matching BeeHexa working example exactly) ===
        item.put("dictionary_type", 3);
        item.put("inventory_item_id", itemId);
        item.put("inventory_item_code", code);
        item.put("inventory_item_name", name);
        item.put("inventory_item_type", 0);  // 0=Hàng hóa

        // === Category ===
        item.put("inventory_item_category_code_list", "HH");
        item.put("inventory_item_category_name_list", "Hàng hóa");

        // === Branch (required) ===
        if (branchId != null && !branchId.isEmpty()) {
            item.put("branch_id", branchId);
        }

        // === Pricing ===
        item.put("minimum_stock", 0.0);
        item.put("unit_price", 0.0);
        item.put("sale_price1", 0.0);
        item.put("sale_price2", 0.0);
        item.put("sale_price3", 0.0);
        item.put("fixed_sale_price", 0.0);
        item.put("fixed_unit_price", 0.0);
        item.put("purchase_last_unit_price", 0.0);
        item.put("purchase_discount_rate", 0.0);
        item.put("is_unit_price_after_tax", false);

        // === Accounting ===
        item.put("inventory_account", "156");
        item.put("cogs_account", "632");
        item.put("sale_account", "5111");
        item.put("discount_account", "5111");
        item.put("sale_off_account", "5111");
        item.put("return_account", "5111");

        // === Tax ===
        item.put("import_tax_rate", 0.0);
        item.put("export_tax_rate", 0.0);

        // === Discount/Allocation ===
        item.put("discount_type", 0);
        item.put("base_on_formula", 0);
        item.put("allocation_type", 0);
        item.put("allocation_time", 0);
        item.put("allocation_account", "5111");

        // === Flags (matching BeeHexa exactly) ===
        item.put("is_system", false);
        item.put("inactive", false);
        item.put("is_follow_serial_number", false);
        item.put("is_allow_duplicate_serial_number", true);
        item.put("is_specific_inventory_item", false);
        item.put("is_group", false);
        item.put("is_valid", false);
        item.put("is_edit_multiple", false);
        item.put("auto_refno", false);

        // === Delete flags ===
        item.put("has_delete_fixed_unit_price", false);
        item.put("has_delete_unit_price", false);
        item.put("has_delete_discount", false);
        item.put("has_delete_unit_convert", false);
        item.put("has_delete_norm", false);
        item.put("has_delete_serial_type", false);

        // === Misc ===
        item.put("quantityBarCode", 1);
        item.put("reftype", 0);
        item.put("reftype_category", 0);
        item.put("excel_row_index", 0);
        item.put("state", 0);

        // org_refid for callback tracking
        item.put("org_refid", "TEST-ITEM-" + timestamp);

        // Override/add any extra fields provided by caller
        if (extraFields != null) {
            item.putAll(extraFields);
        }

        log.info("Creating test inventory item: code={}, name={}, branchId={}, itemId={}", code, name, branchId, itemId);
        return amisApiClient.saveDictionary(3, List.of(item)).block();
    }

    /**
     * Create a customer via saveDictionary(1, data).
     * dictionary_type=1 for customer/vendor (đối tượng).
     *
     * save_dictionary field mapping for type 1:
     *   account_object_id       → UUID (generated)
     *   account_object_code (*) → Customer code (unique)
     *   account_object_name (*) → Customer name
     *   account_object_type     → 0=Vendor, 1=Customer, 2=Employee
     *   is_customer             → true if customer
     *   is_vendor               → true if vendor
     *   address                 → Full address
     *   province_or_city        → Province/City
     *   ward_or_commune         → Ward/Commune
     *   country                 → Country
     *   company_tax_code        → Tax code
     *   tel                     → Phone number
     *   mobile                  → Mobile phone
     *   email_address           → Email address (NOT "email")
     *   contact_name            → Contact person name
     *   pay_account             → Payment account (e.g. "131" for receivable, "331" for payable)
     *   receive_account         → Receivable account (e.g. "131")
     *   branch_id (*)           → Branch ID (required)
     *   inactive                → false = active
     */
    public JsonNode createCustomer(String code, String name, String branchId) {
        return createCustomer(code, name, branchId, null);
    }

    public JsonNode createCustomer(String code, String name, String branchId,
                                    Map<String, Object> extraFields) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        if (code == null || code.isEmpty()) code = "KH-" + timestamp;
        if (name == null || name.isEmpty()) name = "Khach hang Mirror " + timestamp;

        String now = LocalDateTime.now().format(MISA_DATE_FORMAT);

        Map<String, Object> customer = new HashMap<>();
        // Core identity
        customer.put("dictionary_type", 1);
        customer.put("account_object_id", java.util.UUID.randomUUID().toString());
        customer.put("org_refid", "CUST-" + timestamp);
        customer.put("account_object_code", code);
        customer.put("account_object_name", name);

        // Type: 1=Customer (Khách hàng) — matches BeeHexa proven payload
        customer.put("account_object_type", 1);
        customer.put("is_customer", true);
        customer.put("is_vendor", false);

        // Contact info (einvoice_contact_name used for invoice display)
        customer.put("einvoice_contact_name", name);
        customer.put("country", "Việt Nam");

        // Debt/Amount fields
        customer.put("maximize_debt_amount", 0);
        customer.put("closing_amount", 0);
        customer.put("is_remind_debt", true);

        // Flags
        customer.put("inactive", false);

        // Metadata — reftype=9020 matches BeeHexa's customer type
        customer.put("reftype", 9020);
        customer.put("reftype_category", 9020);

        // Branch (required)
        if (branchId != null && !branchId.isEmpty()) {
            customer.put("branch_id", branchId);
        }

        // Override/add any extra fields provided by caller
        if (extraFields != null) {
            customer.putAll(extraFields);
        }

        log.info("Creating customer: code={}, name={}, branchId={}", code, name, branchId);
        return amisApiClient.saveDictionary(1, List.of(customer)).block();
    }

    /**
     * Create a test customer group via saveDictionary(2, data).
     * dictionary_type=2 for customer/vendor group (nhóm đối tượng).
     */
    public JsonNode createAccountObjectGroup(String code, String name, String branchId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        if (code == null || code.isEmpty()) code = "GRP-TEST-" + timestamp;
        if (name == null || name.isEmpty()) name = "Nhom Test Mirror " + timestamp;

        Map<String, Object> group = new HashMap<>();
        group.put("dictionary_type", 2);
        group.put("org_refid", "TEST-GRP-" + timestamp);
        group.put("account_object_group_code", code);
        group.put("account_object_group_name", name);
        group.put("inactive", false);
        if (branchId != null && !branchId.isEmpty()) {
            group.put("branch_id", branchId);
        }

        log.info("Creating test account object group: code={}, name={}", code, name);
        return amisApiClient.saveDictionary(2, List.of(group)).block();
    }

    /**
     * Create a test inventory category via saveDictionary(4, data).
     * dictionary_type=4 for inventory item category (nhóm vật tư).
     */
    public JsonNode createInventoryCategory(String code, String name, String branchId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        if (code == null || code.isEmpty()) code = "CAT-TEST-" + timestamp;
        if (name == null || name.isEmpty()) name = "Nhom VT Test Mirror " + timestamp;

        Map<String, Object> category = new HashMap<>();
        category.put("dictionary_type", 4);
        category.put("org_refid", "TEST-CAT-" + timestamp);
        category.put("inventory_category_code", code);
        category.put("inventory_category_name", name);
        category.put("inactive", false);
        if (branchId != null && !branchId.isEmpty()) {
            category.put("branch_id", branchId);
        }

        log.info("Creating test inventory category: code={}, name={}", code, name);
        return amisApiClient.saveDictionary(4, List.of(category)).block();
    }

    /**
     * Create a test unit of measurement via saveDictionary(6, data).
     * dictionary_type=6 for unit (đơn vị tính).
     */
    public JsonNode createUnit(String name, String branchId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        if (name == null || name.isEmpty()) name = "DVT-Test-" + timestamp;

        Map<String, Object> unit = new HashMap<>();
        unit.put("dictionary_type", 6);
        unit.put("org_refid", "TEST-UNIT-" + timestamp);
        unit.put("unit_name", name);
        unit.put("inactive", false);
        if (branchId != null && !branchId.isEmpty()) {
            unit.put("branch_id", branchId);
        }

        log.info("Creating test unit: name={}", name);
        return amisApiClient.saveDictionary(6, List.of(unit)).block();
    }

    // ==================== VOUCHER OPERATIONS ====================

    /**
     * Create a stock-in (nhap kho) voucher via saveVoucher(7, voucher).
     */
    public JsonNode createStockIn(String itemCode, String itemId, String stockCode, String stockId,
                                   int quantity, long unitPrice, String accountObjectCode) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String now = LocalDateTime.now().format(MISA_DATE_FORMAT);

        // Default warehouse to S00001 if not provided
        String effectiveStockCode = (stockCode != null && !stockCode.isEmpty()) ? stockCode : "S00001";

        Map<String, Object> voucher = new HashMap<>();
        voucher.put("voucher_type", 7);
        voucher.put("org_refid", "STOCKIN-" + timestamp);
        voucher.put("org_refno", "MR-STOCKIN-" + timestamp);
        voucher.put("refdate", now);
        voucher.put("posted_date", now);
        voucher.put("journal_memo", "Nhap kho - " + itemCode);
        voucher.put("currency_id", "VND");
        voucher.put("exchange_rate", 1);
        voucher.put("total_amount", (long) quantity * unitPrice);

        // Vendor/Object code at voucher level
        if (accountObjectCode != null && !accountObjectCode.isEmpty()) {
            voucher.put("account_object_code", accountObjectCode);
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("description", "Nhap kho " + itemCode);
        detail.put("inventory_item_code", itemCode);
        if (itemId != null) detail.put("inventory_item_id", itemId);
        detail.put("quantity", quantity);
        detail.put("unit_price", unitPrice);
        detail.put("amount", (long) quantity * unitPrice);
        detail.put("debit_account", "1561");
        detail.put("credit_account", "331");
        detail.put("stock_code", effectiveStockCode);
        if (stockId != null) detail.put("stock_id", stockId);

        // Vendor/Object code at detail level
        if (accountObjectCode != null && !accountObjectCode.isEmpty()) {
            detail.put("account_object_code", accountObjectCode);
        }

        voucher.put("detail", List.of(detail));

        log.info("Creating stock-in voucher: item={}, qty={}, price={}, warehouse={}, vendor={}",
                 itemCode, quantity, unitPrice, effectiveStockCode, accountObjectCode);
        return amisApiClient.saveVoucher(7, voucher).block();
    }

    /**
     * Create a stock-out (xuat kho) voucher via saveVoucher(8, voucher).
     */
    public JsonNode createStockOut(String itemCode, String itemId, String stockCode, String stockId,
                                    int quantity, long unitPrice) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String now = LocalDateTime.now().format(MISA_DATE_FORMAT);

        Map<String, Object> voucher = new HashMap<>();
        voucher.put("voucher_type", 8);
        voucher.put("org_refid", "STOCKOUT-" + timestamp);
        voucher.put("org_refno", "MR-STOCKOUT-" + timestamp);
        voucher.put("refdate", now);
        voucher.put("posted_date", now);
        voucher.put("journal_memo", "Xuat kho test - " + itemCode);
        voucher.put("currency_id", "VND");
        voucher.put("exchange_rate", 1);
        voucher.put("total_amount", (long) quantity * unitPrice);

        Map<String, Object> detail = new HashMap<>();
        detail.put("description", "Xuat kho " + itemCode);
        detail.put("inventory_item_code", itemCode);
        if (itemId != null) detail.put("inventory_item_id", itemId);
        detail.put("quantity", quantity);
        detail.put("unit_price", unitPrice);
        detail.put("amount", (long) quantity * unitPrice);
        detail.put("debit_account", "632");
        detail.put("credit_account", "1561");
        if (stockId != null) detail.put("stock_id", stockId);
        if (stockCode != null) detail.put("stock_code", stockCode);

        voucher.put("detail", List.of(detail));

        log.info("Creating stock-out voucher: item={}, qty={}, warehouse={}", itemCode, quantity, stockCode);
        return amisApiClient.saveVoucher(8, voucher).block();
    }

    /**
     * Create a stock transfer voucher via saveVoucher(9, voucher).
     */
    public JsonNode createStockTransfer(String itemCode, String itemId,
                                         String fromStockCode, String fromStockId,
                                         String toStockCode, String toStockId,
                                         int quantity, long unitPrice) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String now = LocalDateTime.now().format(MISA_DATE_FORMAT);

        Map<String, Object> voucher = new HashMap<>();
        voucher.put("voucher_type", 9);
        voucher.put("org_refid", "TRANSFER-" + timestamp);
        voucher.put("org_refno", "MR-TRANSFER-" + timestamp);
        voucher.put("refdate", now);
        voucher.put("posted_date", now);
        voucher.put("journal_memo", "Chuyen kho test - " + itemCode);
        voucher.put("currency_id", "VND");
        voucher.put("exchange_rate", 1);
        voucher.put("total_amount", (long) quantity * unitPrice);

        Map<String, Object> detail = new HashMap<>();
        detail.put("description", "Chuyen kho " + itemCode);
        detail.put("inventory_item_code", itemCode);
        if (itemId != null) detail.put("inventory_item_id", itemId);
        detail.put("quantity", quantity);
        detail.put("unit_price", unitPrice);
        detail.put("amount", (long) quantity * unitPrice);
        detail.put("debit_account", "1561");
        detail.put("credit_account", "1561");
        if (fromStockId != null) detail.put("stock_id", fromStockId);
        if (fromStockCode != null) detail.put("stock_code", fromStockCode);
        if (toStockId != null) detail.put("to_stock_id", toStockId);
        if (toStockCode != null) detail.put("to_stock_code", toStockCode);

        voucher.put("detail", List.of(detail));

        log.info("Creating stock transfer: item={}, qty={}, from={} to={}", itemCode, quantity, fromStockCode, toStockCode);
        return amisApiClient.saveVoucher(9, voucher).block();
    }

    /**
     * Create a sales voucher via saveVoucher(13, voucher).
     * Follows the same pattern as MisaVoucherService.submitSalesVoucher.
     */
    public JsonNode createSalesVoucher(String itemCode, String itemId, String stockCode, String stockId,
                                        String customerId, String customerCode,
                                        int quantity, long unitPrice, long costPrice) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String now = LocalDateTime.now().format(MISA_DATE_FORMAT);

        long totalAmount = (long) quantity * unitPrice;
        long totalCost = (long) quantity * costPrice;

        Map<String, Object> voucher = new HashMap<>();
        voucher.put("voucher_type", 13);
        voucher.put("reftype", 3531);
        voucher.put("org_refid", "SALE-TEST-" + timestamp);
        voucher.put("org_refno", "MR-SALE-TEST-" + timestamp);
        voucher.put("refdate", now);
        voucher.put("posted_date", now);
        voucher.put("is_sale_with_outward", true);
        voucher.put("journal_memo", "Ban hang test - " + itemCode);
        voucher.put("currency_id", "VND");
        voucher.put("exchange_rate", 1);
        voucher.put("total_sale_amount", totalAmount);
        voucher.put("total_amount", totalAmount);

        if (customerId != null) voucher.put("account_object_id", customerId);
        if (customerCode != null) voucher.put("account_object_code", customerCode);

        Map<String, Object> detail = new HashMap<>();
        detail.put("description", "Ban hang " + itemCode);
        detail.put("inventory_item_code", itemCode);
        if (itemId != null) detail.put("inventory_item_id", itemId);
        detail.put("quantity", quantity);
        detail.put("unit_price", unitPrice);
        detail.put("amount", totalAmount);
        detail.put("debit_account", "1111");
        detail.put("credit_account", "5111");
        detail.put("cogs_account", "632");
        detail.put("inventory_account", "1561");
        detail.put("outward_unit_price", costPrice);
        detail.put("outward_amount", totalCost);
        detail.put("main_unit_price", costPrice);
        detail.put("main_convert_rate", 1);
        if (stockId != null) {
            detail.put("stock_id", stockId);
            detail.put("stock_code", stockCode);
            detail.put("outward_stock_id", stockId);
            detail.put("outward_stock_code", stockCode);
        }

        voucher.put("detail", List.of(detail));

        log.info("Creating test sales voucher: item={}, qty={}, price={}", itemCode, quantity, unitPrice);
        return amisApiClient.saveVoucher(13, voucher).block();
    }

    // ==================== READ OPERATIONS ====================

    /**
     * Read dictionary data by data_type from MISA AMIS get_dictionary endpoint.
     */
    public JsonNode readDictionary(int dataType, int skip, int take) {
        log.info("Reading dictionary data_type={}, skip={}, take={}", dataType, skip, take);
        return amisApiClient.getDictionaryData(dataType, skip, take).block();
    }

    /**
     * Read inventory balance from MISA AMIS.
     */
    public JsonNode getBalance(int skip, int take, String branchId, String stockId) {
        return amisApiClient.getInventoryBalance(skip, take, branchId, stockId).block();
    }

    /**
     * Poll callback results from MISA AMIS.
     */
    public JsonNode pollCallbackResults(String fromDate, String toDate, int skip, int take) {
        if (fromDate == null || fromDate.isEmpty()) {
            fromDate = LocalDateTime.now().minusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (toDate == null || toDate.isEmpty()) {
            toDate = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return amisApiClient.getCallbackDetailError(fromDate, toDate, skip, take).block();
    }

    // ==================== FULL LIFECYCLE TEST ====================

    /**
     * Run a complete lifecycle test exercising all MISA AMIS operations in sequence.
     * Each step logs its result and continues even if a prior step fails.
     */
    public ObjectNode runFullLifecycleTest() {
        return runFullLifecycleTest(null);
    }

    public ObjectNode runFullLifecycleTest(String branchId) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode steps = objectMapper.createArrayNode();
        String timestamp = String.valueOf(System.currentTimeMillis());

        // Step 1: Authenticate
        ObjectNode step1 = runStep(steps, "1_authenticate", () -> {
            Boolean auth = amisApiClient.authenticate().block();
            ObjectNode r = objectMapper.createObjectNode();
            r.put("authenticated", Boolean.TRUE.equals(auth));
            return r;
        });

        // Step 2: Read existing warehouses
        runStep(steps, "2_read_warehouses", () ->
                amisApiClient.getWarehouses(0, 10).block());

        // Step 3: Create test inventory item
        String testItemCode = "TEST-ITEM-" + timestamp;
        String testItemName = "Lifecycle Test Item " + timestamp;
        runStep(steps, "3_create_inventory_item", () ->
                createInventoryItem(testItemCode, testItemName, branchId));

        // Step 4: Poll for item creation result
        runStep(steps, "4_poll_item_creation", () ->
                pollCallbackResults(null, null, 0, 20));

        // Step 5: Stock in - receive 10 units into default warehouse
        // We need an item that exists in MISA - use the just-created item code
        // In practice the item may not be ready yet (async), so we proceed optimistically
        runStep(steps, "5_stock_in", () ->
                createStockIn(testItemCode, null, null, null, 10, 100000, null));

        // Step 6: Poll for stock-in result
        runStep(steps, "6_poll_stock_in", () ->
                pollCallbackResults(null, null, 0, 20));

        // Step 7: Read balance
        runStep(steps, "7_read_balance", () ->
                getBalance(0, 100, null, null));

        // Step 8: Sales voucher - sell 2 units
        runStep(steps, "8_sales_voucher", () ->
                createSalesVoucher(testItemCode, null, null, null,
                        null, null, 2, 200000, 100000));

        // Step 9: Poll for sales result
        runStep(steps, "9_poll_sales", () ->
                pollCallbackResults(null, null, 0, 20));

        // Step 10: Read balance again
        runStep(steps, "10_read_balance_after_sale", () ->
                getBalance(0, 100, null, null));

        result.set("steps", steps);
        result.put("test_item_code", testItemCode);
        result.put("timestamp", timestamp);
        result.put("completed_at", LocalDateTime.now().format(MISA_DATE_FORMAT));

        // Count pass/fail
        int passed = 0, failed = 0;
        for (JsonNode s : steps) {
            if (s.has("success") && s.get("success").asBoolean()) passed++;
            else failed++;
        }
        result.put("passed", passed);
        result.put("failed", failed);
        result.put("total", steps.size());

        return result;
    }

    /**
     * Run a single test step, catching any exceptions, and append result to the steps array.
     */
    private ObjectNode runStep(ArrayNode steps, String stepName, java.util.function.Supplier<JsonNode> action) {
        ObjectNode stepResult = objectMapper.createObjectNode();
        stepResult.put("step", stepName);
        stepResult.put("started_at", LocalDateTime.now().format(MISA_DATE_FORMAT));

        try {
            JsonNode response = action.get();
            stepResult.set("response", response);

            boolean success = response != null
                    && response.has("Success")
                    && response.get("Success").asBoolean();
            stepResult.put("success", success);

            if (!success && response != null && response.has("ErrorMessage")) {
                stepResult.put("error_message", response.get("ErrorMessage").asText());
            }
        } catch (Exception e) {
            log.error("Lifecycle test step '{}' failed: {}", stepName, e.getMessage(), e);
            stepResult.put("success", false);
            stepResult.put("error", e.getMessage());
            stepResult.put("error_type", e.getClass().getSimpleName());
        }

        stepResult.put("finished_at", LocalDateTime.now().format(MISA_DATE_FORMAT));
        steps.add(stepResult);
        return stepResult;
    }
}
