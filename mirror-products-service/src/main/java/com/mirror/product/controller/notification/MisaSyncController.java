package com.mirror.product.controller.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.client.misa.MisaAmisApiClient;
import com.mirror.product.dto.OrderResponse;
import com.mirror.product.dto.misa.MultiItemSalesVoucherRequest;
import com.mirror.product.dto.notification.MisaCallbackRequest;
import com.mirror.product.dto.notification.MisaCallbackResponse;
import com.mirror.product.entity.misa.MisaBalanceTracker;
import com.mirror.product.entity.misa.MisaBalanceTracker.BalanceTrackerStatus;
import com.mirror.product.entity.misa.MisaCallbackLog;
import com.mirror.product.entity.misa.MisaCustomer;
import com.mirror.product.entity.misa.MisaInventoryItem;
import com.mirror.product.entity.misa.MisaInvoice;
import com.mirror.product.entity.misa.MisaProductCategory;
import com.mirror.product.entity.misa.MisaSyncLog;
import com.mirror.product.entity.misa.MisaWarehouse;
import com.mirror.product.repository.misa.MisaBalanceTrackerRepository;
import com.mirror.product.repository.misa.MisaWarehouseRepository;
import com.mirror.product.repository.misa.MisaCallbackLogRepository;
import com.mirror.product.repository.misa.MisaCustomerRepository;
import com.mirror.product.repository.misa.MisaInventoryItemRepository;
import com.mirror.product.repository.misa.MisaInvoiceRepository;
import com.mirror.product.repository.misa.MisaProductCategoryRepository;
import com.mirror.product.service.MisaOrderIntegrationService;
import com.mirror.product.service.MisaPollingService;
import com.mirror.product.service.OrderService;
import com.mirror.product.service.misa.MisaInventoryBalanceService;
import com.mirror.product.service.misa.MisaVoucherService;
import com.mirror.product.service.notification.MisaCallbackService;
import com.mirror.product.service.notification.MisaSyncService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/misa")
@RequiredArgsConstructor
@Slf4j
public class MisaSyncController {

    private final MisaSyncService misaSyncService;
    private final MisaCallbackService callbackService;
    private final MisaAmisApiClient amisApiClient;
    private final MisaVoucherService misaVoucherService;
    private final MisaInventoryBalanceService misaInventoryBalanceService;
    private final MisaOrderIntegrationService misaOrderIntegrationService;
    private final MisaPollingService misaPollingService;
    private final OrderService orderService;
    private final MisaInventoryItemRepository inventoryItemRepository;
    private final MisaProductCategoryRepository categoryRepository;
    private final MisaCustomerRepository customerRepository;
    private final MisaInvoiceRepository invoiceRepository;
    private final MisaCallbackLogRepository callbackLogRepository;
    private final MisaBalanceTrackerRepository balanceTrackerRepository;
    private final MisaWarehouseRepository warehouseRepository;
    private final ObjectMapper objectMapper;

    /**
     * Trigger manual sync of inventory items.
     */
    @PostMapping("/sync/inventory")
    public ResponseEntity<SyncResponse> syncInventoryItems(@RequestParam(defaultValue = "MANUAL") String triggeredBy) {
        log.info("Manual inventory sync triggered by: {}", triggeredBy);

        try {
            CompletableFuture<MisaSyncLog> syncFuture = misaSyncService.syncInventoryItems(triggeredBy);

            syncFuture.whenComplete((logEntry, error) -> {
                if (error != null) {
                    log.error("Inventory sync failed", error);
                } else {
                    log.info("Inventory sync completed. Success: {}, Failed: {}",
                            logEntry.getSuccessfulRecords(), logEntry.getFailedRecords());
                }
            });

            return ResponseEntity.ok(SyncResponse.builder()
                .success(true)
                .message("Inventory sync started successfully")
                .timestamp(LocalDateTime.now())
                .build());

        } catch (Exception e) {
            log.error("Error starting inventory sync", e);
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                .success(false)
                .message("Failed to start sync: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
        }
    }

    /**
     * Trigger manual sync of product categories.
     */
    @PostMapping("/sync/categories")
    public ResponseEntity<SyncResponse> syncProductCategories(
            @RequestParam(defaultValue = "MANUAL") String triggeredBy,
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        log.info("Manual category sync triggered by: {}, includeInactive: {}", triggeredBy, includeInactive);

        try {
            CompletableFuture<MisaSyncLog> syncFuture = misaSyncService.syncProductCategories(triggeredBy, includeInactive);

            syncFuture.whenComplete((logEntry, error) -> {
                if (error != null) {
                    log.error("Category sync failed", error);
                } else {
                    log.info("Category sync completed. Success: {}, Failed: {}",
                            logEntry.getSuccessfulRecords(), logEntry.getFailedRecords());
                }
            });

            return ResponseEntity.ok(SyncResponse.builder()
                .success(true)
                .message("Category sync started successfully")
                .timestamp(LocalDateTime.now())
                .build());

        } catch (Exception e) {
            log.error("Error starting category sync", e);
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                .success(false)
                .message("Failed to start sync: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
        }
    }

    /**
     * Trigger a full sync (categories + inventory).
     */
    @PostMapping("/sync/all")
    public ResponseEntity<SyncResponse> syncAllData(
            @RequestParam(defaultValue = "MANUAL") String triggeredBy,
            @RequestParam(defaultValue = "false") boolean includeInactiveCategories) {

        log.info("Manual full sync triggered by: {}", triggeredBy);

        try {
            CompletableFuture<List<MisaSyncLog>> syncFuture = misaSyncService.syncAllData(triggeredBy, includeInactiveCategories);

            syncFuture.whenComplete((logs, error) -> {
                if (error != null) {
                    log.error("Full MISA sync failed", error);
                } else {
                    int totalSuccess = logs.stream().mapToInt(MisaSyncLog::getSuccessfulRecords).sum();
                    int totalFailed = logs.stream().mapToInt(MisaSyncLog::getFailedRecords).sum();
                    log.info("Full MISA sync completed. Success: {}, Failed: {}", totalSuccess, totalFailed);
                }
            });

            return ResponseEntity.ok(SyncResponse.builder()
                .success(true)
                .message("Full sync started successfully")
                .timestamp(LocalDateTime.now())
                .build());

        } catch (Exception e) {
            log.error("Error starting full sync", e);
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                .success(false)
                .message("Failed to start full sync: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
        }
    }

    /**
     * Fetch recent sync history.
     */
    @GetMapping("/sync/history")
    public ResponseEntity<List<MisaSyncLog>> getSyncHistory(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(misaSyncService.getSyncHistory(limit));
    }

    /**
     * Fetch sync statistics summary.
     */
    @GetMapping("/sync/stats")
    public ResponseEntity<MisaSyncService.SyncStatistics> getSyncStatistics() {
        return ResponseEntity.ok(misaSyncService.getSyncStatistics());
    }

    /**
     * Get paginated inventory items.
     */
    @GetMapping("/inventory/items")
    public ResponseEntity<Page<MisaInventoryItem>> getInventoryItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "inventoryItemName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "true") boolean activeOnly) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MisaInventoryItem> items;
        if (category != null && !category.trim().isEmpty()) {
            items = inventoryItemRepository.findByInventoryItemCategoryNameContaining(category, pageable);
        } else if (activeOnly) {
            items = inventoryItemRepository.findByIsActiveTrue(pageable);
        } else {
            items = inventoryItemRepository.findAll(pageable);
        }

        return ResponseEntity.ok(items);
    }

    @GetMapping("/inventory/items/{code}")
    public ResponseEntity<MisaInventoryItem> getInventoryItem(@PathVariable String code) {
        Optional<MisaInventoryItem> item = inventoryItemRepository.findByInventoryItemCode(code);
        return item.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/inventory/low-stock")
    public ResponseEntity<List<MisaInventoryItem>> getLowStockItems() {
        return ResponseEntity.ok(misaSyncService.getLowStockItems());
    }

    @GetMapping("/inventory/categories/{categoryId}")
    public ResponseEntity<List<MisaInventoryItem>> getItemsByCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(inventoryItemRepository.findByInventoryItemCategoryId(categoryId));
    }

    @GetMapping("/inventory/categories")
    public ResponseEntity<List<String>> getCategoryNames() {
        return ResponseEntity.ok(inventoryItemRepository.findDistinctCategories());
    }

    @GetMapping("/inventory/brands")
    public ResponseEntity<List<String>> getBrands() {
        return ResponseEntity.ok(inventoryItemRepository.findDistinctBrands());
    }

    @GetMapping("/inventory/search")
    public ResponseEntity<List<MisaInventoryItem>> searchInventoryItems(@RequestParam String query) {
        return ResponseEntity.ok(inventoryItemRepository.findByInventoryItemNameContaining(query));
    }

    @GetMapping("/inventory/needs-sync")
    public ResponseEntity<List<MisaInventoryItem>> getItemsNeedingSync(
            @RequestParam(defaultValue = "24") int hoursAgo) {

        LocalDateTime cutoff = LocalDateTime.now().minusHours(hoursAgo);
        return ResponseEntity.ok(misaSyncService.getItemsNeedingSync(cutoff));
    }

    /**
     * Category queries
     */
    @GetMapping("/categories")
    public ResponseEntity<Page<MisaProductCategory>> getProductCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "categoryName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String name) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MisaProductCategory> categories;

        if (name != null && !name.trim().isEmpty()) {
            categories = categoryRepository.findByCategoryNameContaining(name, pageable);
        } else if (includeInactive) {
            categories = categoryRepository.findAll(pageable);
        } else {
            categories = categoryRepository.findByIsInactiveFalse(pageable);
        }

        return ResponseEntity.ok(categories);
    }

    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<MisaProductCategory> getProductCategory(@PathVariable String categoryId) {
        Optional<MisaProductCategory> category = categoryRepository.findByCategoryId(categoryId);
        return category.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/categories/root")
    public ResponseEntity<List<MisaProductCategory>> getRootCategories() {
        return ResponseEntity.ok(categoryRepository.findRootCategoriesOrdered());
    }

    @GetMapping("/categories/{parentId}/children")
    public ResponseEntity<List<MisaProductCategory>> getChildCategories(@PathVariable String parentId) {
        return ResponseEntity.ok(categoryRepository.findChildCategoriesOrdered(parentId));
    }

    @GetMapping("/categories/grade/{grade}")
    public ResponseEntity<List<MisaProductCategory>> getCategoriesByGrade(@PathVariable Integer grade) {
        return ResponseEntity.ok(categoryRepository.findByGrade(grade));
    }

    @GetMapping("/categories/leaf")
    public ResponseEntity<List<MisaProductCategory>> getLeafCategories() {
        return ResponseEntity.ok(categoryRepository.findByIsLeafTrue());
    }

    @GetMapping("/sync/last-success")
    public ResponseEntity<MisaSyncLog> getLastSuccessfulSync() {
        return misaSyncService.getLastSuccessfulSync()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Trigger manual sync of customers
     */
    @PostMapping("/sync/customers")
    public ResponseEntity<SyncResponse> syncCustomers(@RequestParam(defaultValue = "MANUAL") String triggeredBy) {
        log.info("Manual customers sync triggered by: {}", triggeredBy);

        try {
            CompletableFuture<MisaSyncLog> syncFuture = misaSyncService.syncCustomers(triggeredBy);

            syncFuture.whenComplete((logEntry, error) -> {
                if (error != null) {
                    log.error("Customers sync failed", error);
                } else {
                    log.info("Customers sync completed. Success: {}, Failed: {}",
                            logEntry.getSuccessfulRecords(), logEntry.getFailedRecords());
                }
            });

            return ResponseEntity.ok(SyncResponse.builder()
                .success(true)
                .message("Customers sync started successfully")
                .timestamp(LocalDateTime.now())
                .build());

        } catch (Exception e) {
            log.error("Error starting customers sync", e);
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                .success(false)
                .message("Failed to start sync: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
        }
    }

    /**
     * Trigger manual sync of invoices
     */
    @PostMapping("/sync/invoices")
    public ResponseEntity<SyncResponse> syncInvoices(
            @RequestParam(defaultValue = "MANUAL") String triggeredBy,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        log.info("Manual invoices sync triggered by: {}, date range: {} to {}", triggeredBy, fromDate, toDate);

        try {
            CompletableFuture<MisaSyncLog> syncFuture = misaSyncService.syncInvoices(triggeredBy, fromDate, toDate);

            syncFuture.whenComplete((logEntry, error) -> {
                if (error != null) {
                    log.error("Invoices sync failed", error);
                } else {
                    log.info("Invoices sync completed. Success: {}, Failed: {}",
                            logEntry.getSuccessfulRecords(), logEntry.getFailedRecords());
                }
            });

            return ResponseEntity.ok(SyncResponse.builder()
                .success(true)
                .message("Invoices sync started successfully")
                .timestamp(LocalDateTime.now())
                .build());

        } catch (Exception e) {
            log.error("Error starting invoices sync", e);
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                .success(false)
                .message("Failed to start sync: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
        }
    }

    /**
     * Trigger manual sync of warehouses from MISA AMIS
     */
    @PostMapping("/sync/warehouses")
    public ResponseEntity<SyncResponse> syncWarehouses(@RequestParam(defaultValue = "MANUAL") String triggeredBy) {
        log.info("Manual warehouses sync triggered by: {}", triggeredBy);

        try {
            CompletableFuture<MisaSyncLog> syncFuture = misaSyncService.syncWarehouses(triggeredBy);

            syncFuture.whenComplete((logEntry, error) -> {
                if (error != null) {
                    log.error("Warehouses sync failed", error);
                } else {
                    log.info("Warehouses sync completed. Success: {}, Failed: {}",
                            logEntry.getSuccessfulRecords(), logEntry.getFailedRecords());
                }
            });

            return ResponseEntity.ok(SyncResponse.builder()
                .success(true)
                .message("Warehouses sync started successfully")
                .timestamp(LocalDateTime.now())
                .build());

        } catch (Exception e) {
            log.error("Error starting warehouses sync", e);
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                .success(false)
                .message("Failed to start sync: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
        }
    }

    /**
     * Get paginated warehouses (from local database)
     */
    @GetMapping("/warehouses")
    public ResponseEntity<Page<MisaWarehouse>> getWarehouses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "stockName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "true") boolean activeOnly) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MisaWarehouse> warehouses;
        if (activeOnly) {
            warehouses = warehouseRepository.findByIsInactiveFalse(pageable);
        } else {
            warehouses = warehouseRepository.findAll(pageable);
        }

        return ResponseEntity.ok(warehouses);
    }

    /**
     * Get warehouse by stock ID
     */
    @GetMapping("/warehouses/{stockId}")
    public ResponseEntity<MisaWarehouse> getWarehouseById(@PathVariable String stockId) {
        return warehouseRepository.findByStockId(stockId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get warehouse by stock code
     */
    @GetMapping("/warehouses/code/{stockCode}")
    public ResponseEntity<MisaWarehouse> getWarehouseByCode(@PathVariable String stockCode) {
        return warehouseRepository.findByStockCode(stockCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search warehouses by name or code
     */
    @GetMapping("/warehouses/search")
    public ResponseEntity<List<MisaWarehouse>> searchWarehouses(@RequestParam String query) {
        return ResponseEntity.ok(warehouseRepository.searchByNameOrCode(query));
    }

    /**
     * Get all active warehouses (convenience endpoint for products-service)
     */
    @GetMapping("/warehouses/active")
    public ResponseEntity<List<MisaWarehouse>> getActiveWarehouses() {
        return ResponseEntity.ok(warehouseRepository.findByIsInactiveFalse());
    }

    /**
     * Get paginated customers
     */
    @GetMapping("/customers")
    public ResponseEntity<Page<MisaCustomer>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "customerName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "true") boolean activeOnly) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MisaCustomer> customers;
        if (activeOnly) {
            customers = customerRepository.findByIsActiveTrue(pageable);
        } else {
            customers = customerRepository.findAll(pageable);
        }

        return ResponseEntity.ok(customers);
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<MisaCustomer> getCustomer(@PathVariable String customerId) {
        Optional<MisaCustomer> customer = customerRepository.findByCustomerId(customerId);
        return customer.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/customers/search")
    public ResponseEntity<List<MisaCustomer>> searchCustomers(@RequestParam String query) {
        return ResponseEntity.ok(customerRepository.findByCustomerNameContainingOrPhoneContaining(query, query));
    }

    @GetMapping("/customers/member-levels")
    public ResponseEntity<List<String>> getMemberLevels() {
        return ResponseEntity.ok(customerRepository.findDistinctMemberLevels());
    }

    /**
     * Get paginated invoices
     */
    @GetMapping("/invoices")
    public ResponseEntity<Page<MisaInvoice>> getInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "invoiceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String customerId) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MisaInvoice> invoices;
        if (customerId != null && !customerId.trim().isEmpty()) {
            invoices = invoiceRepository.findByCustomerId(customerId, pageable);
        } else {
            invoices = invoiceRepository.findAll(pageable);
        }

        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<MisaInvoice> getInvoice(@PathVariable String invoiceId) {
        Optional<MisaInvoice> invoice = invoiceRepository.findByInvoiceId(invoiceId);
        return invoice.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/invoices/customer/{customerId}")
    public ResponseEntity<List<MisaInvoice>> getInvoicesByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(invoiceRepository.findByCustomerId(customerId));
    }

    @GetMapping("/invoices/with-debt")
    public ResponseEntity<List<MisaInvoice>> getInvoicesWithDebt() {
        return ResponseEntity.ok(invoiceRepository.findInvoicesWithDebt());
    }

    @GetMapping("/invoices/branches")
    public ResponseEntity<List<String>> getInvoiceBranches() {
        return ResponseEntity.ok(invoiceRepository.findDistinctBranches());
    }

    // ==================== MISA CALLBACK ENDPOINTS ====================

    /**
     * Receive callback from MISA AMIS Open API
     *
     * This endpoint receives asynchronous callbacks from MISA after voucher/dictionary
     * operations complete. According to MISA documentation section 6-1:
     * - data_type: 1=Save, 2=Delete, 3=Update, 6=Export, 7=Dictionary, 8=PaymentRequest, 15=PaymentResponse
     * - Signature verification uses SHA256HMAC with app_id as the key
     */
    @PostMapping("/callback")
    public ResponseEntity<MisaCallbackResponse> handleMisaCallback(
            @RequestBody String rawPayload) {

        log.info("Received MISA callback, payload length: {} bytes", rawPayload != null ? rawPayload.length() : 0);

        try {
            // Parse the raw payload to extract the request
            MisaCallbackRequest request = objectMapper.readValue(rawPayload, MisaCallbackRequest.class);

            // Process the callback
            MisaCallbackResponse response = callbackService.processCallback(request, rawPayload);

            if (Boolean.TRUE.equals(response.getSuccess())) {
                return ResponseEntity.ok(response);
            } else {
                // Return 200 OK even for processing failures to avoid MISA retries
                // The error details are in the response body
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("Error parsing MISA callback payload", e);
            return ResponseEntity.ok(MisaCallbackResponse.error(
                    MisaCallbackResponse.ErrorCodes.INVALID_PAYLOAD,
                    "Failed to parse callback payload: " + e.getMessage()
            ));
        }
    }

    /**
     * Get callback statistics
     */
    @GetMapping("/callback/stats")
    public ResponseEntity<MisaCallbackService.CallbackStatistics> getCallbackStatistics() {
        return ResponseEntity.ok(callbackService.getStatistics());
    }

    /**
     * Get recent callback history
     */
    @GetMapping("/callback/history")
    public ResponseEntity<List<MisaCallbackLog>> getCallbackHistory(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(callbackLogRepository.findTop50ByOrderByReceivedAtDesc());
    }

    /**
     * Get callbacks by data type
     */
    @GetMapping("/callback/by-type/{dataType}")
    public ResponseEntity<Page<MisaCallbackLog>> getCallbacksByType(
            @PathVariable Integer dataType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("receivedAt").descending());
        return ResponseEntity.ok(callbackLogRepository.findByDataType(dataType, pageable));
    }

    /**
     * Get failed callbacks
     */
    @GetMapping("/callback/failed")
    public ResponseEntity<Page<MisaCallbackLog>> getFailedCallbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("receivedAt").descending());
        return ResponseEntity.ok(callbackLogRepository.findByMisaSuccessFalse(pageable));
    }

    /**
     * Get unprocessed callbacks (for retry)
     */
    @GetMapping("/callback/unprocessed")
    public ResponseEntity<List<MisaCallbackLog>> getUnprocessedCallbacks() {
        return ResponseEntity.ok(callbackService.getUnprocessedCallbacks());
    }

    /**
     * Get callbacks with invalid signatures
     */
    @GetMapping("/callback/invalid-signatures")
    public ResponseEntity<Page<MisaCallbackLog>> getInvalidSignatureCallbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("receivedAt").descending());
        return ResponseEntity.ok(callbackLogRepository.findBySignatureValidFalse(pageable));
    }

    /**
     * Get callback by orgRefId
     */
    @GetMapping("/callback/by-ref/{orgRefId}")
    public ResponseEntity<List<MisaCallbackLog>> getCallbacksByOrgRefId(@PathVariable String orgRefId) {
        return ResponseEntity.ok(callbackLogRepository.findByOrgRefId(orgRefId));
    }

    // ==================== INVENTORY BALANCE SYNC & TRACKERS ====================

    /**
     * Trigger manual inventory balance sync from MISA AMIS
     */
    @PostMapping("/sync/inventory-balance")
    public ResponseEntity<SyncResponse> syncInventoryBalance(@RequestParam(defaultValue = "MANUAL") String triggeredBy) {
        log.info("Manual inventory balance sync triggered by: {}", triggeredBy);

        try {
            CompletableFuture<MisaSyncLog> syncFuture = misaInventoryBalanceService.syncInventoryBalance(triggeredBy);

            syncFuture.whenComplete((logEntry, error) -> {
                if (error != null) {
                    log.error("Inventory balance sync failed", error);
                } else {
                    log.info("Inventory balance sync completed. Updated: {}, Failed: {}",
                            logEntry.getSuccessfulRecords(), logEntry.getFailedRecords());
                }
            });

            return ResponseEntity.ok(SyncResponse.builder()
                .success(true)
                .message("Inventory balance sync started successfully")
                .timestamp(LocalDateTime.now())
                .build());

        } catch (Exception e) {
            log.error("Error starting inventory balance sync", e);
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                .success(false)
                .message("Failed to start sync: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
        }
    }

    /**
     * Get paginated balance trackers with optional status filter
     */
    @GetMapping("/balance-trackers")
    public ResponseEntity<Page<MisaBalanceTracker>> getBalanceTrackers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BalanceTrackerStatus status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<MisaBalanceTracker> trackers;
        if (status != null) {
            trackers = balanceTrackerRepository.findByStatus(status, pageable);
        } else {
            trackers = balanceTrackerRepository.findAll(pageable);
        }

        return ResponseEntity.ok(trackers);
    }

    /**
     * Get a single balance tracker by ID
     */
    @GetMapping("/balance-trackers/{id}")
    public ResponseEntity<MisaBalanceTracker> getBalanceTracker(@PathVariable Long id) {
        return balanceTrackerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get balance trackers by voucher reference ID
     */
    @GetMapping("/balance-trackers/by-ref/{orgRefId}")
    public ResponseEntity<List<MisaBalanceTracker>> getBalanceTrackersByRef(@PathVariable String orgRefId) {
        return ResponseEntity.ok(balanceTrackerRepository.findByOrgRefId(orgRefId));
    }

    /**
     * Get all SUBMITTED (pending confirmation) balance trackers
     */
    @GetMapping("/balance-trackers/pending")
    public ResponseEntity<List<MisaBalanceTracker>> getPendingBalanceTrackers() {
        return ResponseEntity.ok(balanceTrackerRepository.findByStatus(BalanceTrackerStatus.SUBMITTED));
    }

    // ==================== MISA AMIS ACT OPEN API ENDPOINTS ====================

    /**
     * Get inventory balance from MISA AMIS
     * Note: Vouchers must be posted (ghi so) in MISA for balance to appear
     * Uses skip/take pagination internally (MISA AMIS API format)
     */
    @GetMapping("/amis/inventory-balance")
    public ResponseEntity<JsonNode> getAmisInventoryBalance(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String stockId) {

        // Convert page/size to skip/take (page 1 = skip 0)
        int skip = (page - 1) * size;
        int take = size;

        log.info("Fetching AMIS inventory balance - skip: {}, take: {}, branchId: {}, stockId: {}",
                skip, take, branchId, stockId);

        try {
            JsonNode result = amisApiClient.getInventoryBalance(skip, take, branchId, stockId).block();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching AMIS inventory balance", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get warehouses list from MISA AMIS
     * Uses skip/take pagination internally (MISA AMIS API format)
     */
    @GetMapping("/amis/warehouses")
    public ResponseEntity<JsonNode> getAmisWarehouses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {

        // Convert page/size to skip/take (page 1 = skip 0)
        int skip = (page - 1) * size;
        int take = size;

        log.info("Fetching AMIS warehouses - skip: {}, take: {}", skip, take);

        try {
            JsonNode result = amisApiClient.getWarehouses(skip, take).block();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching AMIS warehouses", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get inventory items from MISA AMIS
     * Uses skip/take pagination internally (MISA AMIS API format)
     */
    @GetMapping("/amis/inventory-items")
    public ResponseEntity<JsonNode> getAmisInventoryItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {

        // Convert page/size to skip/take (page 1 = skip 0)
        int skip = (page - 1) * size;
        int take = size;

        log.info("Fetching AMIS inventory items - skip: {}, take: {}", skip, take);

        try {
            JsonNode result = amisApiClient.getInventoryItems(skip, take).block();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching AMIS inventory items", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get accounts/customers from MISA AMIS (data_type=1)
     */
    @GetMapping("/amis/accounts")
    public ResponseEntity<JsonNode> getAmisAccounts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {

        int skip = (page - 1) * size;
        int take = size;

        log.info("Fetching AMIS accounts/customers - skip: {}, take: {}", skip, take);

        try {
            JsonNode result = amisApiClient.getAccounts(skip, take).block();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching AMIS accounts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get AMIS authentication status
     */
    @GetMapping("/amis/status")
    public ResponseEntity<AmisStatusResponse> getAmisStatus() {
        return ResponseEntity.ok(AmisStatusResponse.builder()
                .authenticated(amisApiClient.isAuthenticated())
                .baseUrl(amisApiClient.getProperties().getBaseUrl())
                .orgCompanyCode(amisApiClient.getProperties().getOrgCompanyCode())
                .build());
    }

    /**
     * Manually authenticate with MISA AMIS
     */
    @PostMapping("/amis/authenticate")
    public ResponseEntity<SyncResponse> authenticateAmis() {
        log.info("Manual AMIS authentication triggered");

        try {
            Boolean success = amisApiClient.authenticate().block();
            return ResponseEntity.ok(SyncResponse.builder()
                    .success(Boolean.TRUE.equals(success))
                    .message(Boolean.TRUE.equals(success) ? "AMIS authentication successful" : "AMIS authentication failed")
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Error authenticating with AMIS", e);
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                    .success(false)
                    .message("Authentication failed: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    // ==================== MISA AMIS VOUCHER ENDPOINTS ====================

    /**
     * Create Phiếu bán hàng (Sales voucher) with one or more item lines.
     * Uses is_sale_with_outward=true so MISA handles inventory reduction automatically.
     *
     * Example JSON body:
     * {
     *   "items": [
     *     { "itemCode": "ITEM1", "amount": 1000000, "costPrice": 50000, "quantity": 1 },
     *     { "itemCode": "ITEM2", "amount": 2000000, "costPrice": 100000, "quantity": 2 }
     *   ]
     * }
     */
    @PostMapping("/amis/sales")
    public ResponseEntity<JsonNode> submitSalesVoucher(@RequestBody MultiItemSalesVoucherRequest request) {
        log.info("Creating sales voucher with {} items",
                request.getItems() != null ? request.getItems().size() : 0);

        try {
            com.fasterxml.jackson.databind.node.ObjectNode result = misaVoucherService.submitSalesVoucher(request);

            if (result.has("error")) {
                return ResponseEntity.badRequest().body(result);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error creating sales voucher", e);
            return ResponseEntity.internalServerError().body(objectMapper.createObjectNode()
                    .put("error", e.getMessage())
                    .put("type", e.getClass().getSimpleName()));
        }
    }

    /**
     * Read raw callback results from MISA AMIS (diagnostic/debug).
     * Default: last 30 days
     */
    @GetMapping("/amis/callback-results")
    public ResponseEntity<JsonNode> getCallbackResults(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false, defaultValue = "0") int skip,
            @RequestParam(required = false, defaultValue = "100") int take) {
        log.info("Fetching MISA AMIS callback results...");

        try {
            // Default to last 30 days if not specified
            if (fromDate == null || fromDate.isEmpty()) {
                fromDate = LocalDateTime.now().minusDays(30).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            if (toDate == null || toDate.isEmpty()) {
                toDate = LocalDateTime.now().plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }

            JsonNode response = amisApiClient.getCallbackDetailError(fromDate, toDate, skip, take).block();
            log.info("MISA AMIS callback results RAW response: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching callback results", e);
            try {
                JsonNode errorNode = objectMapper.createObjectNode()
                        .put("error", e.getMessage())
                        .put("type", e.getClass().getSimpleName());
                return ResponseEntity.internalServerError().body(errorNode);
            } catch (Exception ex) {
                return ResponseEntity.internalServerError().build();
            }
        }
    }

    // ==================== ORDER MISA INTEGRATION ENDPOINTS ====================

    /**
     * Get orders awaiting MISA SKU creation
     */
    @GetMapping("/orders/awaiting-sku")
    public ResponseEntity<List<OrderResponse>> getOrdersAwaitingSkuCreation() {
        return ResponseEntity.ok(orderService.getOrdersAwaitingMisaSku());
    }

    /**
     * Get orders awaiting MISA invoice submission (PAID but not synced)
     */
    @GetMapping("/orders/awaiting-invoice")
    public ResponseEntity<List<OrderResponse>> getOrdersAwaitingInvoice() {
        return ResponseEntity.ok(orderService.getOrdersPendingMisaSaleRecording());
    }

    /**
     * Retry MISA SKU creation for a specific order
     */
    @PostMapping("/orders/{orderId}/retry-sku")
    public ResponseEntity<SyncResponse> retrySkuCreation(@PathVariable String orderId) {
        log.info("Admin retry SKU creation for order: {}", orderId);
        try {
            misaOrderIntegrationService.retrySkuCreation(orderId);
            return ResponseEntity.ok(SyncResponse.builder()
                    .success(true)
                    .message("SKU creation retry initiated for order: " + orderId)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Error retrying SKU creation for order: {}", orderId, e);
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                    .success(false)
                    .message("Retry failed: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Retry MISA invoice submission for a specific order
     */
    @PostMapping("/orders/{orderId}/retry-invoice")
    public ResponseEntity<SyncResponse> retryInvoiceSubmission(
            @PathVariable String orderId,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        log.info("Admin retry invoice submission for order: {}, force: {}", orderId, force);
        try {
            misaOrderIntegrationService.retryInvoiceSubmission(orderId, force);
            return ResponseEntity.ok(SyncResponse.builder()
                    .success(true)
                    .message("Invoice submission retry initiated for order: " + orderId)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Error retrying invoice submission for order: {}", orderId, e);
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                    .success(false)
                    .message("Retry failed: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Manually trigger MISA callback polling
     */
    @PostMapping("/orders/poll-results")
    public ResponseEntity<SyncResponse> triggerPolling() {
        log.info("Manual MISA callback polling triggered");
        try {
            misaPollingService.pollCallbackResults();
            return ResponseEntity.ok(SyncResponse.builder()
                    .success(true)
                    .message("Polling completed successfully")
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Error during manual polling", e);
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                    .success(false)
                    .message("Polling failed: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    @Data
    @Builder
    public static class AmisStatusResponse {
        private boolean authenticated;
        private String baseUrl;
        private String orgCompanyCode;
    }

    @Data
    @Builder
    public static class SyncResponse {
        private boolean success;
        private String message;
        private LocalDateTime timestamp;
    }
}
