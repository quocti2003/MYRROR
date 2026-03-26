package com.mirror.product.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mirror.product.client.misa.MisaAmisApiClient;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.Order;
import com.mirror.product.entity.OrderItem;
import com.mirror.product.entity.misa.MisaCustomer;
import com.mirror.product.entity.misa.MisaInventoryItem;
import com.mirror.product.entity.misa.MisaSyncLog;
import com.mirror.product.entity.misa.MisaWarehouse;
import com.mirror.product.repository.MirrorProductRepository;
import com.mirror.product.repository.OrderRepository;
import com.mirror.product.repository.misa.MisaCustomerRepository;
import com.mirror.product.repository.misa.MisaInventoryItemRepository;
import com.mirror.product.repository.misa.MisaSyncLogRepository;
import com.mirror.product.repository.misa.MisaWarehouseRepository;
import com.mirror.product.service.misa.MisaInventoryBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MisaOrderIntegrationService {

    private static final int MAX_RETRIES = 5;
    private static final DateTimeFormatter MISA_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final MisaAmisApiClient amisApiClient;
    private final OrderRepository orderRepository;
    private final MirrorProductRepository productRepository;
    private final MisaSyncLogRepository syncLogRepository;
    private final MisaInventoryItemRepository misaInventoryItemRepository;
    private final MisaCustomerRepository misaCustomerRepository;
    private final MisaWarehouseRepository misaWarehouseRepository;
    private final MisaInventoryBalanceService balanceService;

    /**
     * Submit SKU creation to MISA AMIS via save_dictionary (type=2).
     * Called asynchronously after order confirmation.
     */
    @Async
    public void submitSkuCreation(String orderId) {
        log.info("Submitting SKU creation to MISA for order: {}", orderId);

        Order order = orderRepository.findActiveById(orderId).orElse(null);
        if (order == null) {
            log.error("Order not found for SKU creation: {}", orderId);
            return;
        }

        if (Boolean.TRUE.equals(order.getMisaItemCreated())) {
            log.info("Order {} already has MISA SKU created, skipping", orderId);
            return;
        }

        MisaSyncLog syncLog = createSyncLog(MisaSyncLog.SyncType.ORDER_SKU_CREATION, orderId);

        try {
            MirrorProduct product = productRepository.findById(order.getProductId()).orElse(null);
            if (product == null) {
                throw new RuntimeException("Product not found: " + order.getProductId());
            }

            // Skip if product was already pushed to MISA (e.g. at SKU generation time)
            if (Boolean.TRUE.equals(product.getMisaSynced())) {
                log.info("Product {} already synced to MISA, skipping SKU creation for order {}", product.getSkuCode(), orderId);
                order.setMisaItemCreated(true);
                orderRepository.save(order);
                completeSyncLog(syncLog, 1, 0);
                return;
            }

            Map<String, Object> skuItem = buildSkuDictionaryPayload(order, product);

            JsonNode response = amisApiClient.saveDictionary(3, List.of((Object) skuItem)).block();

            if (response != null && response.has("Success") && response.get("Success").asBoolean()) {
                log.info("SKU creation submitted successfully for order: {}", orderId);
                updateOrderSyncAttempt(order);
                completeSyncLog(syncLog, 1, 0);
            } else {
                String errorMsg = response != null && response.has("ErrorMessage")
                        ? response.get("ErrorMessage").asText()
                        : "Unknown MISA error";
                throw new RuntimeException("MISA save_dictionary failed: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("Failed to submit SKU creation for order: {}", orderId, e);
            handleSkuCreationFailure(order, e);
            failSyncLog(syncLog, e.getMessage());
        }
    }

    /**
     * Submit sales voucher to MISA AMIS via save endpoint.
     *
     * Configuration:
     * - voucher_type=13 (Bán hàng/Sales)
     * - reftype=3531 (Bán hàng hóa trong nước - tiền mặt)
     * - is_sale_with_outward=true (creates linked warehouse export)
     *
     * This creates a sales proposal in MISA that requires 1-step manual processing:
     * - MISA staff processes the proposal with "Thu tiền ngay" option selected
     * - This transforms BH → PT with "Đã thanh toán" and "Đã xuất đủ" statuses
     * - Product details are preserved in the resulting cash receipt
     *
     * Note: API creates proposals first, "Thu tiền ngay" is only available during
     * the approval step in MISA UI, not as an API parameter.
     *
     * Called asynchronously after payment is fully received.
     */
    @Async
    public void submitSalesInvoice(String orderId) {
        log.info("Submitting sales invoice to MISA for order: {}", orderId);

        Order order = orderRepository.findActiveById(orderId).orElse(null);
        if (order == null) {
            log.error("Order not found for invoice submission: {}", orderId);
            return;
        }

        if (Boolean.TRUE.equals(order.getMisaSaleRecorded())) {
            log.info("Order {} already has MISA sale recorded, skipping", orderId);
            return;
        }

        MisaSyncLog syncLog = createSyncLog(MisaSyncLog.SyncType.ORDER_INVOICE_SUBMIT, orderId);

        try {
            MirrorProduct product = productRepository.findById(order.getProductId()).orElse(null);

            Map<String, Object> voucher = buildSalesVoucherPayload(order, product);
            log.info("MISA voucher payload for order {}: {}", orderId, voucher);

            JsonNode response = amisApiClient.saveVoucher(13, voucher).block();

            if (response != null && response.has("Success") && response.get("Success").asBoolean()) {
                log.info("Sales invoice submitted successfully for order: {}", orderId);
                updateOrderSyncAttempt(order);
                completeSyncLog(syncLog, 1, 0);

                // Create balance trackers for each line item
                createBalanceTrackersForOrder(order, voucher);
            } else {
                String errorMsg = response != null && response.has("ErrorMessage")
                        ? response.get("ErrorMessage").asText()
                        : "Unknown MISA error";
                throw new RuntimeException("MISA save voucher failed: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("Failed to submit sales invoice for order: {}", orderId, e);
            handleInvoiceSubmissionFailure(order, e);
            failSyncLog(syncLog, e.getMessage());
        }
    }

    /**
     * Retry SKU creation for a specific order (admin action).
     */
    @Transactional
    public void retrySkuCreation(String orderId) {
        Order order = orderRepository.findActiveById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (Boolean.TRUE.equals(order.getMisaItemCreated())) {
            throw new IllegalStateException("Order already has MISA SKU created");
        }

        if (order.getMisaSkuRetryCount() >= MAX_RETRIES) {
            order.setMisaSkuRetryCount(0);
            orderRepository.save(order);
        }

        submitSkuCreation(orderId);
    }

    /**
     * Retry invoice submission for a specific order (admin action).
     * @param force If true, resets misaSaleRecorded flag and resubmits
     */
    @Transactional
    public void retryInvoiceSubmission(String orderId, boolean force) {
        Order order = orderRepository.findActiveById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (Boolean.TRUE.equals(order.getMisaSaleRecorded())) {
            if (!force) {
                throw new IllegalStateException("Order already has MISA sale recorded");
            }
            order.setMisaSaleRecorded(false);
        }

        if (order.getMisaInvoiceRetryCount() >= MAX_RETRIES) {
            order.setMisaInvoiceRetryCount(0);
        }
        orderRepository.save(order);

        submitSalesInvoice(orderId);
    }

    /**
     * Retry all failed SKU creations (scheduler action).
     */
    public void retryFailedSkuCreations() {
        List<Order> pendingOrders = orderRepository.findConfirmedOrdersAwaitingSkuCreation(MAX_RETRIES);

        if (pendingOrders.isEmpty()) {
            return;
        }

        log.info("Retrying SKU creation for {} orders", pendingOrders.size());
        for (Order order : pendingOrders) {
            submitSkuCreation(order.getId());
        }
    }

    /**
     * Retry all failed invoice submissions (scheduler action).
     */
    public void retryFailedInvoiceSubmissions() {
        List<Order> pendingOrders = orderRepository.findPaidOrdersAwaitingInvoice(MAX_RETRIES);

        if (pendingOrders.isEmpty()) {
            return;
        }

        log.info("Retrying invoice submission for {} orders", pendingOrders.size());
        for (Order order : pendingOrders) {
            submitSalesInvoice(order.getId());
        }
    }

    /**
     * Build SKU dictionary payload from a MirrorProduct only (no Order dependency).
     * Uses skuCode (barcode) as inventory_item_code and descriptiveCode as org_refid.
     * Public so other services (e.g. GeneratedSkuService) can reuse it.
     */
    public Map<String, Object> buildSkuDictionaryPayload(MirrorProduct product) {
        Map<String, Object> item = new HashMap<>();

        // Core identity
        item.put("dictionary_type", 3);
        item.put("inventory_item_id", java.util.UUID.randomUUID().toString());
        item.put("org_refid", product.getDescriptiveCode() != null ? product.getDescriptiveCode() : product.getSkuCode());
        item.put("inventory_item_code", product.getSkuCode());
        item.put("inventory_item_name", product.getItemName() != null ? product.getItemName() : product.getSkuCode());
        item.put("inventory_item_type", 0);

        // Branch (required)
        if (amisApiClient.getAmisProperties().getBranchId() != null
                && !amisApiClient.getAmisProperties().getBranchId().isEmpty()) {
            item.put("branch_id", amisApiClient.getAmisProperties().getBranchId());
        }

        // Category
        if (product.getCategory() != null) {
            item.put("inventory_item_category_code_list", product.getCategory());
        }
        item.put("inventory_item_category_name_list", "Hàng hóa");

        // Unit of Measurement (Đơn vị tính)
        item.put("unit_name", "Cái");

        // Pricing
        item.put("sale_price1", product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);
        item.put("unit_price", product.getCost() != null ? product.getCost().doubleValue() : 0.0);
        item.put("sale_price2", 0.0);
        item.put("sale_price3", 0.0);
        item.put("fixed_sale_price", 0.0);
        item.put("fixed_unit_price", 0.0);
        item.put("purchase_last_unit_price", 0.0);
        item.put("purchase_discount_rate", 0.0);
        item.put("minimum_stock", 0.0);
        item.put("is_unit_price_after_tax", false);

        // Accounting
        item.put("inventory_account", "156");
        item.put("cogs_account", "632");
        item.put("sale_account", "5111");
        item.put("discount_account", "5111");
        item.put("sale_off_account", "5111");
        item.put("return_account", "5111");

        // Tax
        item.put("import_tax_rate", 0.0);
        item.put("export_tax_rate", 0.0);

        // Discount/Allocation
        item.put("discount_type", 0);
        item.put("base_on_formula", 0);
        item.put("allocation_type", 0);
        item.put("allocation_time", 0);
        item.put("allocation_account", "5111");

        // Flags
        item.put("is_system", false);
        item.put("inactive", false);
        item.put("is_follow_serial_number", false);
        item.put("is_allow_duplicate_serial_number", true);
        item.put("is_specific_inventory_item", false);
        item.put("is_group", false);
        item.put("is_valid", false);
        item.put("is_edit_multiple", false);
        item.put("auto_refno", false);

        // Delete flags
        item.put("has_delete_fixed_unit_price", false);
        item.put("has_delete_unit_price", false);
        item.put("has_delete_discount", false);
        item.put("has_delete_unit_convert", false);
        item.put("has_delete_norm", false);
        item.put("has_delete_serial_type", false);

        // Misc
        item.put("quantityBarCode", 1);
        item.put("reftype", 0);
        item.put("reftype_category", 0);
        item.put("excel_row_index", 0);
        item.put("state", 0);

        return item;
    }

    private Map<String, Object> buildSkuDictionaryPayload(Order order, MirrorProduct product) {
        Map<String, Object> item = buildSkuDictionaryPayload(product);
        // Override org_refid with order ID for order-based submissions
        item.put("org_refid", order.getId());
        return item;
    }

    private Map<String, Object> buildSalesVoucherPayload(Order order, MirrorProduct product) {
        Map<String, Object> voucher = new HashMap<>();

        // voucher_type=13 (Bán hàng/Sales) creates proposals in MISA UI
        voucher.put("voucher_type", 13);

        // reftype=3531 creates "Bán hàng hóa trong nước - tiền mặt" type
        voucher.put("reftype", 3531);

        // is_sale_with_outward=true creates linked warehouse export
        voucher.put("is_sale_with_outward", true);

        voucher.put("org_refid", order.getId());
        voucher.put("org_refno", "MR-" + order.getId());

        String now = LocalDateTime.now().format(MISA_DATE_FORMAT);
        voucher.put("refdate", now);
        voucher.put("posted_date", now);
        voucher.put("journal_memo", "Sales invoice for order " + order.getId());
        voucher.put("currency_id", order.getCurrency() != null ? order.getCurrency() : "VND");
        voucher.put("exchange_rate", 1);

        java.math.BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : java.math.BigDecimal.ZERO;
        voucher.put("total_sale_amount", totalAmount);
        voucher.put("total_amount", totalAmount);

        // Look up default warehouse
        MisaWarehouse defaultWarehouse = getDefaultWarehouse();
        if (defaultWarehouse != null && defaultWarehouse.getBranchId() != null) {
            voucher.put("branch_id", defaultWarehouse.getBranchId());
        }

        // Customer info on voucher header
        if (order.getCustomerName() != null) {
            voucher.put("contact_name", order.getCustomerName());
        }
        if (order.getCustomerPhone() != null) {
            voucher.put("contact_mobile", order.getCustomerPhone());
        }
        if (order.getCustomerEmail() != null) {
            voucher.put("contact_email", order.getCustomerEmail());
        }

        Optional<MisaCustomer> misaCustomer = findMisaCustomer(order);
        if (misaCustomer.isPresent()) {
            MisaCustomer customer = misaCustomer.get();
            voucher.put("account_object_id", customer.getCustomerId());
            voucher.put("account_object_code", customer.getCustomerCode());
            voucher.put("account_object_name", customer.getCustomerName());
            if (customer.getAddress() != null) {
                voucher.put("contact_address", customer.getAddress());
            }
        }

        // Build detail lines from order items (multi-item support)
        List<Map<String, Object>> detailLines = new java.util.ArrayList<>();
        Set<OrderItem> orderItems = order.getItems();

        if (orderItems != null && !orderItems.isEmpty()) {
            // Multi-item order: one detail line per OrderItem
            for (OrderItem orderItem : orderItems) {
                Map<String, Object> detailLine = buildDetailLine(orderItem, defaultWarehouse, misaCustomer.orElse(null));
                detailLines.add(detailLine);
            }
            log.info("Built {} detail lines from order items for order {}", detailLines.size(), order.getId());
        } else {
            // Legacy single-product order: use order.productId
            Map<String, Object> detailLine = buildDetailLineFromProduct(
                    product, order.getQuantity(), totalAmount, defaultWarehouse, misaCustomer.orElse(null));
            detailLines.add(detailLine);
            log.info("Built 1 detail line from legacy product for order {}", order.getId());
        }

        voucher.put("detail", detailLines);
        return voucher;
    }

    /**
     * Build a voucher detail line from an OrderItem (multi-item path).
     */
    private Map<String, Object> buildDetailLine(OrderItem orderItem, MisaWarehouse warehouse, MisaCustomer customer) {
        Map<String, Object> line = new HashMap<>();
        int quantity = orderItem.getQuantity() != null ? orderItem.getQuantity() : 1;
        java.math.BigDecimal unitPrice = orderItem.getUnitPrice() != null ? orderItem.getUnitPrice() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal lineTotal = orderItem.getTotalPrice() != null ? orderItem.getTotalPrice()
                : unitPrice.multiply(java.math.BigDecimal.valueOf(quantity));

        line.put("description", orderItem.getProductName() != null
                ? orderItem.getProductName() : "Item " + orderItem.getProductId());
        line.put("quantity", quantity);
        line.put("unit_price", unitPrice);
        line.put("amount", lineTotal);

        line.put("debit_account", "131");
        line.put("credit_account", "5111");
        line.put("cogs_account", "632");
        line.put("inventory_account", "1561");

        // Resolve MISA inventory item from OrderItem's product
        MirrorProduct itemProduct = orderItem.getProduct();
        if (itemProduct == null) {
            itemProduct = productRepository.findById(orderItem.getProductId()).orElse(null);
        }
        populateInventoryFields(line, itemProduct);
        populateWarehouseFields(line, warehouse);

        if (customer != null) {
            line.put("account_object_code", customer.getCustomerCode());
            line.put("account_object_name", customer.getCustomerName());
        }

        return line;
    }

    /**
     * Build a voucher detail line from a single MirrorProduct (legacy path).
     */
    private Map<String, Object> buildDetailLineFromProduct(MirrorProduct product, Integer quantity,
                                                            java.math.BigDecimal totalAmount,
                                                            MisaWarehouse warehouse, MisaCustomer customer) {
        Map<String, Object> line = new HashMap<>();
        int qty = quantity != null ? quantity : 1;
        java.math.BigDecimal unitPrice = totalAmount.divide(
                java.math.BigDecimal.valueOf(Math.max(1, qty)), 2, java.math.RoundingMode.HALF_UP);

        line.put("description", product != null && product.getItemName() != null
                ? product.getItemName() : "Order item");
        line.put("quantity", qty);
        line.put("unit_price", unitPrice);
        line.put("amount", totalAmount);

        line.put("debit_account", "131");
        line.put("credit_account", "5111");
        line.put("cogs_account", "632");
        line.put("inventory_account", "1561");

        populateInventoryFields(line, product);
        populateWarehouseFields(line, warehouse);

        if (customer != null) {
            line.put("account_object_code", customer.getCustomerCode());
            line.put("account_object_name", customer.getCustomerName());
        }

        return line;
    }

    /**
     * Populate MISA inventory item fields on a detail line from a MirrorProduct.
     */
    private void populateInventoryFields(Map<String, Object> line, MirrorProduct product) {
        MisaInventoryItem inventoryItem = null;
        if (product != null && product.getMisaItemCode() != null) {
            inventoryItem = misaInventoryItemRepository
                    .findByInventoryItemCode(product.getMisaItemCode()).orElse(null);
        }
        if (inventoryItem == null && product != null && product.getSkuCode() != null) {
            inventoryItem = misaInventoryItemRepository
                    .findByInventoryItemCode(product.getSkuCode()).orElse(null);
        }

        if (inventoryItem != null) {
            if (inventoryItem.getInventoryItemId() != null) {
                line.put("inventory_item_id", inventoryItem.getInventoryItemId());
            }
            line.put("inventory_item_code", inventoryItem.getInventoryItemCode());
            line.put("inventory_item_name", inventoryItem.getInventoryItemName());
            if (inventoryItem.getUnitId() != null) {
                line.put("unit_id", inventoryItem.getUnitId());
            }
            if (inventoryItem.getUnitName() != null) {
                line.put("unit_name", inventoryItem.getUnitName());
            }

            // Cost price fields
            if (inventoryItem.getCostPrice() != null) {
                java.math.BigDecimal costPrice = inventoryItem.getCostPrice();
                int qty = line.containsKey("quantity") ? (int) line.get("quantity") : 1;
                line.put("outward_unit_price", costPrice);
                line.put("outward_amount", costPrice.multiply(java.math.BigDecimal.valueOf(qty)));
                line.put("main_unit_price", costPrice);
                line.put("main_convert_rate", 1);
            }
        } else if (product != null && product.getMisaItemCode() != null) {
            line.put("inventory_item_code", product.getMisaItemCode());
        }
    }

    /**
     * Populate warehouse/stock fields on a detail line.
     */
    private void populateWarehouseFields(Map<String, Object> line, MisaWarehouse warehouse) {
        if (warehouse != null) {
            line.put("stock_id", warehouse.getStockId());
            line.put("stock_code", warehouse.getStockCode());
            line.put("outward_stock_id", warehouse.getStockId());
            line.put("outward_stock_code", warehouse.getStockCode());
        }
    }

    private MisaWarehouse getDefaultWarehouse() {
        List<MisaWarehouse> warehouses = misaWarehouseRepository.findByIsInactiveFalse();
        if (warehouses.isEmpty()) {
            return null;
        }
        return warehouses.stream()
                .filter(w -> "Khomirrorfuturediamond".equals(w.getStockCode()))
                .findFirst()
                .orElse(warehouses.get(0));
    }

    /**
     * Create balance trackers for each line item in the submitted voucher.
     */
    private void createBalanceTrackersForOrder(Order order, Map<String, Object> voucher) {
        String orgRefId = (String) voucher.get("org_refid");
        String orgRefNo = (String) voucher.get("org_refno");
        MisaWarehouse wh = getDefaultWarehouse();
        String stockId = wh != null ? wh.getStockId() : null;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detailLines = (List<Map<String, Object>>) voucher.get("detail");
        if (detailLines == null) return;

        for (Map<String, Object> line : detailLines) {
            try {
                String itemCode = (String) line.get("inventory_item_code");
                String itemId = (String) line.get("inventory_item_id");
                int quantity = line.containsKey("quantity") ? ((Number) line.get("quantity")).intValue() : 1;

                if (itemCode == null) continue;

                balanceService.createBalanceTracker(itemCode, itemId, stockId, orgRefId, orgRefNo, 13, quantity);
            } catch (Exception e) {
                log.warn("Failed to create balance tracker for order {} line item: {}", order.getId(), e.getMessage());
            }
        }
    }

    /**
     * Find a MISA customer matching the order's customer info.
     * Tries phone number match first, then name search.
     */
    private Optional<MisaCustomer> findMisaCustomer(Order order) {
        if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
            String phone = order.getCustomerPhone().replaceAll("[^0-9+]", "");
            Optional<MisaCustomer> byPhone = misaCustomerRepository.findFirstByPhone(phone);
            if (byPhone.isPresent()) {
                return byPhone;
            }
            // Try normalized (remove leading +84, replace with 0)
            String normalized = phone.startsWith("+84") ? "0" + phone.substring(3) : phone;
            Optional<MisaCustomer> byNormalized = misaCustomerRepository.findFirstByNormalizedPhone(normalized);
            if (byNormalized.isPresent()) {
                return byNormalized;
            }
        }
        return Optional.empty();
    }

    @Transactional
    protected void handleSkuCreationFailure(Order order, Exception e) {
        Order freshOrder = orderRepository.findById(order.getId()).orElse(order);
        freshOrder.setMisaSkuRetryCount(freshOrder.getMisaSkuRetryCount() + 1);
        freshOrder.setMisaLastSyncAttempt(LocalDateTime.now());
        orderRepository.save(freshOrder);
    }

    @Transactional
    protected void handleInvoiceSubmissionFailure(Order order, Exception e) {
        Order freshOrder = orderRepository.findById(order.getId()).orElse(order);
        freshOrder.setMisaInvoiceRetryCount(freshOrder.getMisaInvoiceRetryCount() + 1);
        freshOrder.setMisaLastSyncAttempt(LocalDateTime.now());
        orderRepository.save(freshOrder);
    }

    @Transactional
    protected void updateOrderSyncAttempt(Order order) {
        Order freshOrder = orderRepository.findById(order.getId()).orElse(order);
        freshOrder.setMisaLastSyncAttempt(LocalDateTime.now());
        orderRepository.save(freshOrder);
    }

    private MisaSyncLog createSyncLog(MisaSyncLog.SyncType syncType, String orderId) {
        MisaSyncLog syncLog = new MisaSyncLog();
        syncLog.setSyncType(syncType);
        syncLog.setStatus(MisaSyncLog.SyncStatus.STARTED);
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setTriggeredBy("ORDER:" + orderId);
        syncLog.setTotalRecords(1);
        return syncLogRepository.save(syncLog);
    }

    private void completeSyncLog(MisaSyncLog syncLog, int successful, int failed) {
        syncLog.setProcessedRecords(successful + failed);
        syncLog.setSuccessfulRecords(successful);
        syncLog.setFailedRecords(failed);
        syncLog.markAsCompleted();
        syncLogRepository.save(syncLog);
    }

    private void failSyncLog(MisaSyncLog syncLog, String errorMessage) {
        syncLog.setProcessedRecords(1);
        syncLog.setSuccessfulRecords(0);
        syncLog.setFailedRecords(1);
        syncLog.markAsFailed(errorMessage);
        syncLogRepository.save(syncLog);
    }
}
