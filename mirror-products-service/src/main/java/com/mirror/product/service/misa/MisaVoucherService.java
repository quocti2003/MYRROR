package com.mirror.product.service.misa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mirror.product.client.misa.MisaAmisApiClient;
import com.mirror.product.dto.misa.MultiItemSalesVoucherRequest;
import com.mirror.product.entity.misa.MisaBalanceTracker;
import com.mirror.product.entity.misa.MisaWarehouse;
import com.mirror.product.repository.misa.MisaWarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MisaVoucherService {

    private static final DateTimeFormatter MISA_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final MisaAmisApiClient amisApiClient;
    private final MisaWarehouseRepository warehouseRepository;
    private final MisaInventoryBalanceService balanceService;
    private final ObjectMapper objectMapper;

    /**
     * Fetch an inventory item from MISA AMIS API by item code.
     *
     * @param itemCode the inventory item code to search for
     * @return the matching JsonNode from MISA AMIS, or null if not found
     */
    public JsonNode findInventoryItemFromAmis(String itemCode) {
        JsonNode inventoryResponse = amisApiClient.getInventoryItems(0, 100).block();
        if (inventoryResponse == null || !inventoryResponse.has("Success") || !inventoryResponse.get("Success").asBoolean()) {
            return null;
        }

        try {
            String dataStr = inventoryResponse.get("Data").asText();
            JsonNode inventoryItems = objectMapper.readTree(dataStr);
            for (JsonNode item : inventoryItems) {
                if (item.has("inventory_item_code") &&
                    item.get("inventory_item_code").asText().equals(itemCode)) {
                    return item;
                }
            }
        } catch (Exception e) {
            log.error("Error parsing MISA inventory items", e);
        }
        return null;
    }

    /**
     * Fetch the first customer (code starting with "KH") from MISA AMIS API.
     *
     * @return the matching customer JsonNode, or null if none found
     */
    public JsonNode findDefaultCustomer() {
        try {
            JsonNode accountsResponse = amisApiClient.getAccounts(0, 100).block();
            if (accountsResponse != null && accountsResponse.has("Success") && accountsResponse.get("Success").asBoolean()) {
                String accDataStr = accountsResponse.get("Data").asText();
                JsonNode accounts = objectMapper.readTree(accDataStr);
                for (JsonNode acc : accounts) {
                    String accCode = acc.has("account_object_code") ? acc.get("account_object_code").asText() : "";
                    if (accCode.startsWith("KH") && acc.has("account_object_id")) {
                        return acc;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching default customer from MISA AMIS", e);
        }
        return null;
    }

    /**
     * Get the preferred warehouse from local DB.
     * Prefers "Khomirrorfuturediamond" warehouse, otherwise uses first available active warehouse.
     *
     * @return the preferred MisaWarehouse, or null if none found
     */
    public MisaWarehouse getDefaultWarehouse() {
        List<MisaWarehouse> warehouses = warehouseRepository.findByIsInactiveFalse();
        if (warehouses.isEmpty()) {
            return null;
        }
        return warehouses.stream()
                .filter(w -> "Khomirrorfuturediamond".equals(w.getStockCode()))
                .findFirst()
                .orElse(warehouses.get(0));
    }

    /**
     * Build and submit a sales voucher to MISA AMIS with one or more item lines.
     *
     * @param request contains list of line items (supports single or multiple items)
     * @return ObjectNode with submission result
     */
    public ObjectNode submitSalesVoucher(MultiItemSalesVoucherRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "No items provided");
            return error;
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String now = LocalDateTime.now().format(MISA_DATE_FORMAT);

        // Resolve all items from AMIS first
        List<Map.Entry<MultiItemSalesVoucherRequest.LineItem, JsonNode>> resolvedItems = new java.util.ArrayList<>();
        for (MultiItemSalesVoucherRequest.LineItem lineItem : request.getItems()) {
            JsonNode misaItem = findInventoryItemFromAmis(lineItem.getItemCode());
            if (misaItem == null) {
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Item not found in MISA AMIS: " + lineItem.getItemCode());
                error.put("hint", "All items must exist in MISA AMIS");
                return error;
            }
            resolvedItems.add(Map.entry(lineItem, misaItem));
        }

        // Calculate totals across all lines
        BigDecimal grandTotalAmount = BigDecimal.ZERO;
        for (var entry : resolvedItems) {
            MultiItemSalesVoucherRequest.LineItem li = entry.getKey();
            grandTotalAmount = grandTotalAmount.add(li.getAmount().multiply(BigDecimal.valueOf(li.getQuantity())));
        }

        // Build voucher header
        Map<String, Object> voucher = new HashMap<>();
        voucher.put("voucher_type", 13);
        voucher.put("reftype", 3531);
        String orgRefId = "SALE-" + timestamp;
        String orgRefNo = "MR-SALE-" + timestamp;
        voucher.put("org_refid", orgRefId);
        voucher.put("org_refno", orgRefNo);
        voucher.put("refdate", now);
        voucher.put("posted_date", now);
        voucher.put("is_sale_with_outward", true);
        voucher.put("journal_memo", "Bán hàng - " + resolvedItems.size() + " items");
        voucher.put("currency_id", "VND");
        voucher.put("exchange_rate", 1);
        voucher.put("total_sale_amount", grandTotalAmount);
        voucher.put("total_amount", grandTotalAmount);

        // Use branch_id from first item if available
        JsonNode firstMisaItem = resolvedItems.get(0).getValue();
        if (firstMisaItem.has("branch_id")) {
            voucher.put("branch_id", firstMisaItem.get("branch_id").asText());
        }

        // Customer
        JsonNode customer = findDefaultCustomer();
        if (customer != null) {
            voucher.put("account_object_id", customer.get("account_object_id").asText());
            voucher.put("account_object_code", customer.get("account_object_code").asText());
            if (customer.has("account_object_name")) {
                voucher.put("account_object_name", customer.get("account_object_name").asText());
            }
        }

        // Warehouse
        MisaWarehouse wh = getDefaultWarehouse();
        String stockId = wh != null ? wh.getStockId() : null;
        String stockCode = wh != null ? wh.getStockCode() : null;

        // Build detail lines - one per item
        List<Map<String, Object>> detailLines = new java.util.ArrayList<>();
        for (var entry : resolvedItems) {
            MultiItemSalesVoucherRequest.LineItem li = entry.getKey();
            JsonNode misaItem = entry.getValue();

            BigDecimal lineAmount = li.getAmount().multiply(BigDecimal.valueOf(li.getQuantity()));
            BigDecimal lineCost = li.getCostPrice().multiply(BigDecimal.valueOf(li.getQuantity()));

            Map<String, Object> detailLine = new HashMap<>();
            detailLine.put("description", misaItem.has("inventory_item_name")
                    ? misaItem.get("inventory_item_name").asText() : li.getItemCode());
            detailLine.put("quantity", li.getQuantity());
            detailLine.put("unit_price", li.getAmount());
            detailLine.put("amount", lineAmount);
            detailLine.put("debit_account", "1111");
            detailLine.put("credit_account", "5111");
            detailLine.put("cogs_account", "632");
            detailLine.put("inventory_account", "1561");

            detailLine.put("outward_unit_price", li.getCostPrice());
            detailLine.put("outward_amount", lineCost);
            detailLine.put("main_unit_price", li.getCostPrice());
            detailLine.put("main_convert_rate", 1);

            detailLine.put("inventory_item_id", misaItem.get("inventory_item_id").asText());
            detailLine.put("inventory_item_code", misaItem.get("inventory_item_code").asText());
            detailLine.put("inventory_item_name", misaItem.get("inventory_item_name").asText());
            detailLine.put("unit_id", misaItem.get("unit_id").asText());
            detailLine.put("unit_name", misaItem.has("unit_name") ? misaItem.get("unit_name").asText() : "Cái");

            if (stockId != null) {
                detailLine.put("stock_id", stockId);
                detailLine.put("stock_code", stockCode);
                detailLine.put("outward_stock_id", stockId);
                detailLine.put("outward_stock_code", stockCode);
            }

            detailLines.add(detailLine);
        }

        voucher.put("detail", detailLines);

        log.info("Sending sales voucher ({} lines): {}", detailLines.size(), voucher);
        JsonNode response = amisApiClient.saveVoucher(13, voucher).block();
        log.info("MISA sales voucher response: {}", response);

        boolean success = response != null && response.has("Success") && response.get("Success").asBoolean();

        ObjectNode result = objectMapper.createObjectNode();
        result.put("voucher_type", "Phiếu bán hàng");
        result.put("reftype", 3531);
        result.put("org_refid", orgRefId);
        result.put("org_refno", orgRefNo);
        result.put("item_count", resolvedItems.size());
        result.put("total_amount", grandTotalAmount);
        result.put("warehouse_code", wh != null ? wh.getStockCode() : null);
        result.put("is_sale_with_outward", true);
        result.set("misa_response", response);
        result.put("success", success);

        if (success) {
            for (var entry : resolvedItems) {
                try {
                    MultiItemSalesVoucherRequest.LineItem li = entry.getKey();
                    JsonNode misaItem = entry.getValue();
                    String itemId = misaItem.has("inventory_item_id") ? misaItem.get("inventory_item_id").asText() : null;
                    MisaBalanceTracker tracker = balanceService.createBalanceTracker(
                            li.getItemCode(), itemId, stockId,
                            orgRefId, orgRefNo, 13, li.getQuantity());
                    log.info("Created balance tracker {} for item {}", tracker.getId(), li.getItemCode());
                } catch (Exception e) {
                    log.warn("Failed to create balance tracker for voucher {}: {}", orgRefId, e.getMessage());
                }
            }
        }

        return result;
    }
}
