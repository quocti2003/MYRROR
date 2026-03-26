package com.mirror.product.service.misa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.client.misa.MisaAmisApiClient;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.misa.MisaBalanceTracker;
import com.mirror.product.entity.misa.MisaBalanceTracker.BalanceTrackerStatus;
import com.mirror.product.entity.misa.MisaInventoryItem;
import com.mirror.product.entity.misa.MisaSyncLog;
import com.mirror.product.repository.MirrorProductRepository;
import com.mirror.product.repository.misa.MisaBalanceTrackerRepository;
import com.mirror.product.repository.misa.MisaInventoryItemRepository;
import com.mirror.product.repository.misa.MisaSyncLogRepository;
import com.mirror.product.repository.misa.MisaWarehouseRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MisaInventoryBalanceService {

    /** Stock code of the main warehouse used for all Mirror system vouchers. */
    private static final String MAIN_WAREHOUSE_CODE = "Khomirrorfuturediamond";

    private final MisaAmisApiClient amisApiClient;
    private final MisaInventoryItemRepository inventoryItemRepository;
    private final MirrorProductRepository mirrorProductRepository;
    private final MisaBalanceTrackerRepository balanceTrackerRepository;
    private final MisaSyncLogRepository syncLogRepository;
    private final MisaWarehouseRepository warehouseRepository;
    private final ObjectMapper objectMapper;

    /** Cached main warehouse stock_id, resolved on startup. */
    private String mainWarehouseStockId;

    @PostConstruct
    void resolveMainWarehouse() {
        warehouseRepository.findByStockCode(MAIN_WAREHOUSE_CODE).ifPresent(wh -> {
            mainWarehouseStockId = wh.getStockId();
            log.info("Main warehouse resolved: {} (stockId={})", MAIN_WAREHOUSE_CODE, mainWarehouseStockId);
        });
        if (mainWarehouseStockId == null) {
            log.warn("Main warehouse '{}' not found in local DB — balance sync will fetch all warehouses until resolved", MAIN_WAREHOUSE_CODE);
        }
    }

    @Async
    public CompletableFuture<MisaSyncLog> syncInventoryBalance(String triggeredBy) {
        MisaSyncLog syncLog = new MisaSyncLog();
        syncLog.setSyncType(MisaSyncLog.SyncType.INVENTORY_BALANCE);
        syncLog.setStatus(MisaSyncLog.SyncStatus.STARTED);
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setTriggeredBy(triggeredBy);
        syncLog = syncLogRepository.save(syncLog);

        try {
            syncLog.setStatus(MisaSyncLog.SyncStatus.IN_PROGRESS);
            syncLogRepository.save(syncLog);

            // Resolve main warehouse if not yet cached (e.g. first sync after fresh DB)
            if (mainWarehouseStockId == null) {
                resolveMainWarehouse();
            }

            log.info("Syncing inventory balance for main warehouse: {} (stockId={})",
                    MAIN_WAREHOUSE_CODE, mainWarehouseStockId);

            Map<String, Integer> warehouseQuantities = new HashMap<>();
            int totalRecords = 0;
            int skip = 0;
            int take = 100;
            boolean hasMore = true;

            while (hasMore) {
                // Filter by main warehouse stockId — only track balance for the Mirror system warehouse
                JsonNode response = amisApiClient.getInventoryBalance(skip, take, null, mainWarehouseStockId).block();

                if (response == null || !response.has("Success") || !response.get("Success").asBoolean()) {
                    String errorMsg = response != null && response.has("ErrorMessage")
                            ? response.get("ErrorMessage").asText()
                            : "Unknown error fetching inventory balance";
                    log.error("AMIS inventory balance fetch failed at skip={}: {}", skip, errorMsg);
                    break;
                }

                if (!response.has("Data") || response.get("Data").isNull()) {
                    break;
                }

                String dataStr = response.get("Data").asText();
                if (dataStr == null || dataStr.isEmpty() || "null".equals(dataStr)) {
                    break;
                }

                JsonNode items = objectMapper.readTree(dataStr);
                if (!items.isArray() || items.isEmpty()) {
                    break;
                }

                for (JsonNode item : items) {
                    String itemCode = item.has("inventory_item_code")
                            ? item.get("inventory_item_code").asText() : null;
                    if (itemCode == null || itemCode.isEmpty()) {
                        continue;
                    }

                    int quantity = item.has("quantity_balance")
                            ? item.get("quantity_balance").asInt(0)
                            : (item.has("closing_quantity") ? item.get("closing_quantity").asInt(0) : 0);

                    // No aggregation — each record is already scoped to main warehouse
                    warehouseQuantities.put(itemCode, quantity);
                    totalRecords++;
                }

                if (items.size() < take) {
                    hasMore = false;
                } else {
                    skip += take;
                }
            }

            log.info("Fetched {} balance records for main warehouse '{}'", totalRecords, MAIN_WAREHOUSE_CODE);

            int successCount = 0;
            int failCount = 0;

            for (Map.Entry<String, Integer> entry : warehouseQuantities.entrySet()) {
                try {
                    updateItemBalance(entry.getKey(), entry.getValue());
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to update balance for item {}: {}", entry.getKey(), e.getMessage());
                    failCount++;
                }
            }

            checkBalanceTrackers();

            syncLog.setTotalRecords(totalRecords);
            syncLog.setProcessedRecords(warehouseQuantities.size());
            syncLog.setSuccessfulRecords(successCount);
            syncLog.setFailedRecords(failCount);
            syncLog.markAsCompleted();
            syncLog.setDetails("Main warehouse '" + MAIN_WAREHOUSE_CODE + "': " + warehouseQuantities.size()
                    + " items from " + totalRecords + " balance records");
            syncLogRepository.save(syncLog);

            log.info("Inventory balance sync completed. Updated: {}, Failed: {}", successCount, failCount);
            return CompletableFuture.completedFuture(syncLog);

        } catch (Exception e) {
            log.error("Inventory balance sync failed", e);
            syncLog.markAsFailed(e.getMessage());
            syncLogRepository.save(syncLog);
            return CompletableFuture.completedFuture(syncLog);
        }
    }

    @Transactional
    public void updateItemBalance(String itemCode, int totalQuantity) {
        Optional<MisaInventoryItem> itemOpt = findInventoryItemByCode(itemCode);
        if (itemOpt.isPresent()) {
            MisaInventoryItem item = itemOpt.get();
            item.setQuantityOnHand(totalQuantity);
            item.setQuantityAvailable(totalQuantity - (item.getQuantityReserved() != null ? item.getQuantityReserved() : 0));
            inventoryItemRepository.save(item);
            String resolvedCode = item.getInventoryItemCode();
            log.debug("Updated MisaInventoryItem {} quantity to {}", resolvedCode, totalQuantity);

            // Use the resolved (local DB) code for Product lookups
            Optional<MirrorProduct> productOpt = mirrorProductRepository.findByMisaItemCode(resolvedCode);
            if (productOpt.isEmpty() && !resolvedCode.equals(itemCode)) {
                productOpt = mirrorProductRepository.findByMisaItemCode(itemCode);
            }
            if (productOpt.isPresent()) {
                MirrorProduct product = productOpt.get();
                product.setStockQuantity(totalQuantity);
                mirrorProductRepository.save(product);
                log.debug("Updated MirrorProduct {} stockQuantity to {}", product.getSkuCode(), totalQuantity);
            }
        }
    }

    @Transactional
    public void checkBalanceTrackers() {
        List<MisaBalanceTracker> submittedTrackers = balanceTrackerRepository.findByStatus(BalanceTrackerStatus.SUBMITTED);
        LocalDateTime now = LocalDateTime.now();

        for (MisaBalanceTracker tracker : submittedTrackers) {
            try {
                Optional<MisaInventoryItem> itemOpt = findInventoryItemByCode(tracker.getInventoryItemCode());
                if (itemOpt.isEmpty()) {
                    tracker.setPollAttempts(tracker.getPollAttempts() + 1);
                    tracker.setLastPolledAt(now);
                    tracker.setNotes("Inventory item not found: " + tracker.getInventoryItemCode());
                    balanceTrackerRepository.save(tracker);
                    continue;
                }

                int currentQuantity = itemOpt.get().getQuantityOnHand() != null
                        ? itemOpt.get().getQuantityOnHand() : 0;

                // Balance is scoped to main warehouse — confirm when current <= expected
                if (currentQuantity <= tracker.getExpectedQuantityAfter()) {
                    tracker.setStatus(BalanceTrackerStatus.CONFIRMED);
                    tracker.setConfirmedAt(now);
                    tracker.setActualQuantityAfter(currentQuantity);
                    tracker.setLastPolledAt(now);
                    tracker.setPollAttempts(tracker.getPollAttempts() + 1);
                    balanceTrackerRepository.save(tracker);
                    log.info("Balance tracker {} CONFIRMED for item {} (expected<={}, actual={})",
                            tracker.getId(), tracker.getInventoryItemCode(),
                            tracker.getExpectedQuantityAfter(), currentQuantity);
                } else if (tracker.getTimeoutAt() != null && tracker.getTimeoutAt().isBefore(now)) {
                    tracker.setStatus(BalanceTrackerStatus.TIMEOUT);
                    tracker.setActualQuantityAfter(currentQuantity);
                    tracker.setLastPolledAt(now);
                    tracker.setPollAttempts(tracker.getPollAttempts() + 1);
                    tracker.setNotes("Timed out. Expected<=" + tracker.getExpectedQuantityAfter()
                            + " but actual=" + currentQuantity);
                    balanceTrackerRepository.save(tracker);
                    log.warn("Balance tracker {} TIMEOUT for item {} (expected<={}, actual={})",
                            tracker.getId(), tracker.getInventoryItemCode(),
                            tracker.getExpectedQuantityAfter(), currentQuantity);
                } else {
                    tracker.setPollAttempts(tracker.getPollAttempts() + 1);
                    tracker.setLastPolledAt(now);
                    balanceTrackerRepository.save(tracker);
                }
            } catch (Exception e) {
                log.error("Error checking balance tracker {}: {}", tracker.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public MisaBalanceTracker createBalanceTracker(String itemCode, String itemId, String stockId,
                                                    String orgRefId, String orgRefNo,
                                                    Integer voucherType, int quantity) {
        int currentQuantity = 0;
        Optional<MisaInventoryItem> itemOpt = findInventoryItemByCode(itemCode);
        if (itemOpt.isPresent() && itemOpt.get().getQuantityOnHand() != null) {
            currentQuantity = itemOpt.get().getQuantityOnHand();
        }

        MisaBalanceTracker tracker = new MisaBalanceTracker();
        tracker.setInventoryItemCode(itemCode);
        tracker.setInventoryItemId(itemId);
        tracker.setStockId(stockId);
        tracker.setOrgRefId(orgRefId);
        tracker.setOrgRefNo(orgRefNo);
        tracker.setVoucherType(voucherType);
        tracker.setQuantityBefore(currentQuantity);
        tracker.setQuantityChange(quantity);
        tracker.setExpectedQuantityAfter(currentQuantity - quantity);
        tracker.setStatus(BalanceTrackerStatus.SUBMITTED);
        tracker.setSubmittedAt(LocalDateTime.now());
        tracker.setTimeoutAt(LocalDateTime.now().plusHours(24));
        tracker.setPollAttempts(0);

        tracker = balanceTrackerRepository.save(tracker);
        log.info("Created balance tracker {} for item {} (before={}, expectedAfter={}, change=-{})",
                tracker.getId(), itemCode, currentQuantity, tracker.getExpectedQuantityAfter(), quantity);

        return tracker;
    }

    /**
     * Find inventory item by code, with fallback for MISA code variant suffixes.
     * MISA AMIS balance/voucher APIs append a color suffix (e.g. "-WH", "-BL")
     * that the dictionary API does not include. Try exact match first, then strip
     * the last hyphen segment.
     */
    private Optional<MisaInventoryItem> findInventoryItemByCode(String itemCode) {
        Optional<MisaInventoryItem> result = inventoryItemRepository.findByInventoryItemCode(itemCode);
        if (result.isPresent()) {
            return result;
        }
        // Strip last suffix (e.g. "7670885253429-WH" -> "7670885253429")
        int lastDash = itemCode.lastIndexOf('-');
        if (lastDash > 0) {
            String baseCode = itemCode.substring(0, lastDash);
            result = inventoryItemRepository.findByInventoryItemCode(baseCode);
            if (result.isPresent()) {
                log.debug("Resolved item code {} -> {} via suffix stripping", itemCode, baseCode);
                return result;
            }
        }
        return Optional.empty();
    }
}
