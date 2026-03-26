package com.mirror.product.service.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.client.misa.MisaApiClient;
import com.mirror.product.client.misa.MisaAmisApiClient;
import com.mirror.product.dto.notification.MisaApiResponse;
import com.mirror.product.dto.notification.MisaCustomerDto;
import com.mirror.product.dto.notification.MisaInventoryItemDto;
import com.mirror.product.dto.notification.MisaInvoiceDto;
import com.mirror.product.dto.notification.MisaProductCategoryDto;
import com.mirror.product.entity.misa.MisaCustomer;
import com.mirror.product.entity.misa.MisaInventoryItem;
import com.mirror.product.entity.misa.MisaInvoice;
import com.mirror.product.entity.misa.MisaProductCategory;
import com.mirror.product.entity.misa.MisaSyncLog;
import com.mirror.product.entity.misa.MisaWarehouse;
import com.mirror.product.repository.misa.MisaCustomerRepository;
import com.mirror.product.repository.misa.MisaInventoryItemRepository;
import com.mirror.product.repository.misa.MisaInvoiceRepository;
import com.mirror.product.repository.misa.MisaProductCategoryRepository;
import com.mirror.product.repository.misa.MisaSyncLogRepository;
import com.mirror.product.repository.misa.MisaWarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MisaSyncService {

    private final MisaApiClient misaApiClient;
    private final MisaAmisApiClient amisApiClient;
    private final ObjectMapper objectMapper;
    private final MisaInventoryItemRepository inventoryItemRepository;
    private final MisaProductCategoryRepository categoryRepository;
    private final MisaCustomerRepository customerRepository;
    private final MisaInvoiceRepository invoiceRepository;
    private final MisaWarehouseRepository warehouseRepository;
    private final MisaSyncLogRepository syncLogRepository;

    @Value("${misa.sync.batch-size:100}")
    private int batchSize;

    /**
     * Synchronize inventory items from MISA API
     */
    @Async
    @Transactional
    public CompletableFuture<MisaSyncLog> syncInventoryItems(String triggeredBy) {
        log.info("Starting MISA inventory sync triggered by: {}", triggeredBy);

        // Create sync log
        MisaSyncLog syncLog = new MisaSyncLog();
        syncLog.setSyncType(MisaSyncLog.SyncType.INVENTORY_ITEMS);
        syncLog.setStatus(MisaSyncLog.SyncStatus.STARTED);
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setTriggeredBy(triggeredBy);
        syncLog = syncLogRepository.save(syncLog);

        final MisaSyncLog finalSyncLog = syncLog;

        int pageSize = Math.max(1, batchSize);

        return misaApiClient.getInventoryItems(1, pageSize, "Code", "1", true, null, true)
            .map(response -> processInventoryItemsResponse(response, finalSyncLog))
            .doOnError(error -> handleSyncError(finalSyncLog, error))
            .toFuture();
    }

    /**
     * Process the MISA inventory items response
     */
    private MisaSyncLog processInventoryItemsResponse(MisaApiResponse<MisaInventoryItemDto> response, MisaSyncLog syncLog) {
        try {
            if (response.getSuccess() != null && response.getSuccess()) {
                syncLog.setStatus(MisaSyncLog.SyncStatus.IN_PROGRESS);
                syncLog.setTotalRecords(response.getTotal());
                syncLog = syncLogRepository.save(syncLog);

                InventorySyncCounters counters = syncInventoryAcrossPages(response, Math.max(1, batchSize), syncLog);

                syncLog.setProcessedRecords(counters.processedRecords);
                syncLog.setSuccessfulRecords(counters.successfulRecords);
                syncLog.setFailedRecords(counters.failedRecords);
                syncLog.markAsCompleted();

                log.info("MISA inventory sync completed - Successful: {}, Failed: {} (processed {})",
                        counters.successfulRecords,
                        counters.failedRecords,
                        counters.processedRecords);

            } else {
                syncLog.markAsFailed("MISA API returned error: " + response.getErrorMessage());
                log.error("MISA inventory sync failed: {}", response.getErrorMessage());
            }
        } catch (Exception e) {
            syncLog.markAsFailed("Unexpected error during sync: " + e.getMessage());
            log.error("Error processing MISA inventory response", e);
        }

        return syncLogRepository.save(syncLog);
    }

    private InventorySyncCounters syncInventoryAcrossPages(
            MisaApiResponse<MisaInventoryItemDto> firstPageResponse,
            int pageSize,
            MisaSyncLog syncLog) {

        int totalRecords = firstPageResponse.getTotal() != null ? firstPageResponse.getTotal() : 0;
        int successful = 0;
        int failed = 0;
        int processed = 0;

        int currentPage = 1;
        MisaApiResponse<MisaInventoryItemDto> currentResponse = firstPageResponse;

        while (currentResponse != null && currentResponse.getData() != null && !currentResponse.getData().isEmpty()) {
            List<MisaInventoryItemDto> items = currentResponse.getData();

            for (MisaInventoryItemDto itemDto : items) {
                try {
                    syncInventoryItem(itemDto);
                    successful++;
                } catch (Exception e) {
                    log.error("Error syncing item: {}", itemDto.getCode(), e);
                    failed++;
                }
            }

            processed += items.size();

            // Stop if we've processed everything reported by MISA
            if (totalRecords > 0 && processed >= totalRecords) {
                break;
            }

            currentPage++;
            MisaApiResponse<MisaInventoryItemDto> nextResponse = fetchInventoryPage(currentPage, pageSize);

            if (nextResponse == null || nextResponse.getData() == null || nextResponse.getData().isEmpty()) {
                log.info("No additional inventory items returned after page {}", currentPage - 1);
                break;
            }

            currentResponse = nextResponse;
        }

        if (totalRecords == 0) {
            totalRecords = processed;
        }

        syncLog.setTotalRecords(totalRecords);

        return new InventorySyncCounters(processed, successful, failed);
    }

    private MisaApiResponse<MisaInventoryItemDto> fetchInventoryPage(int page, int pageSize) {
        try {
            return misaApiClient
                .getInventoryItems(page, pageSize, "Code", "1", true, null, true)
                .block();
        } catch (Exception e) {
            log.error("Failed to fetch MISA inventory page {}", page, e);
            return null;
        }
    }

    private String resolveCategoryCode(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        return categoryRepository.findByCategoryId(categoryId)
                .map(MisaProductCategory::getCategoryCode)
                .map(this::normalizeCategoryCode)
                .orElse(null);
    }

    private String resolveMaterial(MisaInventoryItemDto dto) {
        String materialFromItem = normalizeMaterial(dto.getColourCode());
        if (materialFromItem != null) {
            return materialFromItem;
        }

        if (dto.getListDetail() != null) {
            for (MisaInventoryItemDto.ListDetailDto detail : dto.getListDetail()) {
                String material = normalizeMaterial(detail.getMaterial());
                if (material != null) {
                    return material;
                }

                material = normalizeMaterial(detail.getColourCode());
                if (material != null) {
                    return material;
                }
            }
        }

        return null;
    }

    private String normalizeCategoryCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    private String normalizeMaterial(String material) {
        if (material == null) {
            return null;
        }
        String trimmed = material.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    private static class InventorySyncCounters {
        final int processedRecords;
        final int successfulRecords;
        final int failedRecords;

        InventorySyncCounters(int processedRecords, int successfulRecords, int failedRecords) {
            this.processedRecords = processedRecords;
            this.successfulRecords = successfulRecords;
            this.failedRecords = failedRecords;
        }
    }

    /**
     * Sync individual inventory item
     */
    @Transactional
    public void syncInventoryItem(MisaInventoryItemDto itemDto) {
        try {
            Optional<MisaInventoryItem> existingItem =
                inventoryItemRepository.findByInventoryItemCode(itemDto.getCode());

            MisaInventoryItem item;
            if (existingItem.isPresent()) {
                item = existingItem.get();
                updateInventoryItemFromDto(item, itemDto);
                item.setSyncStatus(MisaInventoryItem.SyncStatus.UPDATED);
            } else {
                item = createInventoryItemFromDto(itemDto);
                item.setSyncStatus(MisaInventoryItem.SyncStatus.SYNCED);
            }

            item.setLastSyncDate(LocalDateTime.now());
            item.setSyncErrorMessage(null);

            inventoryItemRepository.save(item);

        } catch (Exception e) {
            log.error("Error syncing inventory item: {}", itemDto.getCode(), e);

            // Try to update error status if item exists
            inventoryItemRepository.findByInventoryItemCode(itemDto.getCode())
                .ifPresent(item -> {
                    item.setSyncStatus(MisaInventoryItem.SyncStatus.ERROR);
                    item.setSyncErrorMessage(e.getMessage());
                    inventoryItemRepository.save(item);
                });

            throw e;
        }
    }

    /**
     * Create new inventory item from DTO
     */
    private MisaInventoryItem createInventoryItemFromDto(MisaInventoryItemDto dto) {
        MisaInventoryItem item = new MisaInventoryItem();
        updateInventoryItemFromDto(item, dto);
        return item;
    }

    /**
     * Update inventory item from DTO
     */
    private void updateInventoryItemFromDto(MisaInventoryItem item, MisaInventoryItemDto dto) {
        item.setInventoryItemCode(dto.getCode());
        item.setInventoryItemName(dto.getName());
        item.setInventoryItemId(dto.getId());
        item.setInventoryItemCategoryId(dto.getItemCategoryId());
        item.setInventoryItemCategoryName(dto.getItemCategoryName());
        item.setInventoryItemCategoryCode(resolveCategoryCode(dto.getItemCategoryId()));
        item.setBrandName(null); // Not available in new structure
        item.setUnitName(dto.getUnitName());
        item.setUnitId(dto.getUnitId());
        item.setSalePrice(dto.getSellingPrice());
        item.setCostPrice(dto.getCostPrice());

        // Calculate quantities from ListDetail if available
        if (dto.getListDetail() != null && !dto.getListDetail().isEmpty()) {
            // Get first detail item for inventory calculations
            MisaInventoryItemDto.ListDetailDto firstDetail = dto.getListDetail().get(0);
            if (firstDetail.getInventories() != null && !firstDetail.getInventories().isEmpty()) {
                MisaInventoryItemDto.InventoryDto firstInventory = firstDetail.getInventories().get(0);
                item.setQuantityOnHand(firstInventory.getOnHand() != null ? firstInventory.getOnHand().intValue() : 0);
                item.setQuantityReserved(firstInventory.getOrdered() != null ? firstInventory.getOrdered().intValue() : 0);
                item.setQuantityAvailable(firstInventory.getOnHand() != null ? firstInventory.getOnHand().intValue() : 0);
            }
        }

        item.setMinimumStock(null); // Not available in new structure
        item.setMaximumStock(null); // Not available in new structure
        item.setWeight(null); // Not available in new structure
        item.setDimensions(null); // Not available in new structure
        item.setColor(dto.getColor());
        item.setSize(dto.getSize());
        item.setMaterial(resolveMaterial(dto));
        item.setDescription(dto.getDescription());
        item.setIsActive(dto.getInactive() != null ? !dto.getInactive() : true);
        item.setIsForSale(dto.getIsItem());
        item.setIsForPurchase(dto.getIsItem());
        item.setTaxRate(null); // Not available in new structure
        item.setWarrantyPeriod(null); // Not available in new structure
        item.setWarrantyUnit(null); // Not available in new structure
        item.setOriginCountry(null); // Not available in new structure
        item.setManufacturer(null); // Not available in new structure

        // Get barcode from first detail item if available
        if (dto.getListDetail() != null && !dto.getListDetail().isEmpty()) {
            MisaInventoryItemDto.ListDetailDto firstDetail = dto.getListDetail().get(0);
            item.setBarcode(firstDetail.getBarcode());
        }

        item.setImageUrl(dto.getPicture());
        item.setMisaLastModified(dto.getModifiedDate() != null ? dto.getModifiedDate().toLocalDateTime() : null);
    }

    /**
     * Handle sync errors
     */
    private void handleSyncError(MisaSyncLog syncLog, Throwable error) {
        log.error("MISA sync failed", error);
        syncLog.markAsFailed(error.getMessage());
        syncLogRepository.save(syncLog);
    }

    /**
     * Get sync status
     */
    public List<MisaSyncLog> getSyncHistory(int limit) {
        return syncLogRepository.findAll().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(limit)
            .toList();
    }

    /**
     * Get last successful sync
     */
    public Optional<MisaSyncLog> getLastSuccessfulSync() {
        return syncLogRepository.findTopBySyncTypeOrderByCreatedAtDesc(MisaSyncLog.SyncType.INVENTORY_ITEMS)
            .filter(log -> log.getStatus() == MisaSyncLog.SyncStatus.COMPLETED);
    }

    /**
     * Get items that need syncing (older than cutoff time)
     */
    public List<MisaInventoryItem> getItemsNeedingSync(LocalDateTime cutoffTime) {
        return inventoryItemRepository.findItemsNeedingSync(cutoffTime);
    }

    /**
     * Get low stock items
     */
    public List<MisaInventoryItem> getLowStockItems() {
        return inventoryItemRepository.findLowStockItems();
    }

    /**
     * Synchronize product categories from MISA API
     */
    @Async
    @Transactional
    public CompletableFuture<MisaSyncLog> syncProductCategories(String triggeredBy, boolean includeInactive) {
        log.info("Starting MISA categories sync triggered by: {}, include inactive: {}", triggeredBy, includeInactive);

        // Create sync log
        MisaSyncLog syncLog = new MisaSyncLog();
        syncLog.setSyncType(MisaSyncLog.SyncType.CATEGORIES);
        syncLog.setStatus(MisaSyncLog.SyncStatus.STARTED);
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setTriggeredBy(triggeredBy);
        syncLog = syncLogRepository.save(syncLog);

        final MisaSyncLog finalSyncLog = syncLog;

        return misaApiClient.getProductCategories(includeInactive)
            .map(response -> processCategoriesResponse(response, finalSyncLog))
            .doOnError(error -> handleSyncError(finalSyncLog, error))
            .toFuture();
    }

    /**
     * Process the MISA categories response
     */
    private MisaSyncLog processCategoriesResponse(MisaApiResponse<MisaProductCategoryDto> response, MisaSyncLog syncLog) {
        try {
            if (response.getSuccess() != null && response.getSuccess()) {
                List<MisaProductCategoryDto> categories = response.getData();

                syncLog.setStatus(MisaSyncLog.SyncStatus.IN_PROGRESS);
                syncLog.setTotalRecords(response.getTotal());
                syncLog.setProcessedRecords(categories.size());
                syncLog = syncLogRepository.save(syncLog);

                int successful = 0;
                int failed = 0;

                for (MisaProductCategoryDto categoryDto : categories) {
                    try {
                        syncProductCategory(categoryDto);
                        successful++;
                    } catch (Exception e) {
                        log.error("Error syncing category: {}", categoryDto.getCode(), e);
                        failed++;
                    }
                }

                syncLog.setSuccessfulRecords(successful);
                syncLog.setFailedRecords(failed);
                syncLog.markAsCompleted();

                log.info("MISA categories sync completed - Successful: {}, Failed: {}", successful, failed);

            } else {
                syncLog.markAsFailed("MISA API returned error: " + response.getErrorMessage());
                log.error("MISA categories sync failed: {}", response.getErrorMessage());
            }
        } catch (Exception e) {
            syncLog.markAsFailed("Unexpected error during sync: " + e.getMessage());
            log.error("Error processing MISA categories response", e);
        }

        return syncLogRepository.save(syncLog);
    }

    /**
     * Sync individual product category
     */
    @Transactional
    public void syncProductCategory(MisaProductCategoryDto categoryDto) {
        try {
            Optional<MisaProductCategory> existingCategory =
                categoryRepository.findByCategoryId(categoryDto.getId());

            MisaProductCategory category;
            if (existingCategory.isPresent()) {
                category = existingCategory.get();
                updateCategoryFromDto(category, categoryDto);
                category.setSyncStatus(MisaProductCategory.SyncStatus.UPDATED);
            } else {
                category = createCategoryFromDto(categoryDto);
                category.setSyncStatus(MisaProductCategory.SyncStatus.SYNCED);
            }

            category.setLastSyncDate(LocalDateTime.now());
            category.setSyncErrorMessage(null);

            categoryRepository.save(category);

        } catch (Exception e) {
            log.error("Error syncing category: {}", categoryDto.getCode(), e);

            // Try to update error status if category exists
            categoryRepository.findByCategoryId(categoryDto.getId())
                .ifPresent(category -> {
                    category.setSyncStatus(MisaProductCategory.SyncStatus.ERROR);
                    category.setSyncErrorMessage(e.getMessage());
                    categoryRepository.save(category);
                });

            throw e;
        }
    }


    /**
     * Create new category from DTO
     */
    private MisaProductCategory createCategoryFromDto(MisaProductCategoryDto dto) {
        MisaProductCategory category = new MisaProductCategory();
        updateCategoryFromDto(category, dto);
        return category;
    }

    /**
     * Update category from DTO
     */
    private void updateCategoryFromDto(MisaProductCategory category, MisaProductCategoryDto dto) {
        category.setCategoryId(dto.getId());
        category.setCategoryCode(dto.getCode());
        category.setCategoryName(dto.getName());
        category.setParentId(dto.getParentId());
        category.setGrade(dto.getGrade());
        category.setIsInactive(dto.getInactive() != null ? dto.getInactive() : false);
        category.setIsLeaf(dto.getIsLeaf() != null ? dto.getIsLeaf() : false);
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder());
        category.setFullPath(dto.getFullPath());
        category.setLevelNames(dto.getLevelNames());
        category.setMisaLastModified(dto.getLastModified() != null ? dto.getLastModified().toLocalDateTime() : null);
    }

    /**
     * Synchronize both categories and inventory items
     */
    @Async
    @Transactional
    public CompletableFuture<List<MisaSyncLog>> syncAllData(String triggeredBy, boolean includeInactiveCategories) {
        log.info("Starting full MISA sync (categories + inventory items) triggered by: {}", triggeredBy);

        // First sync categories, then inventory items
        return syncProductCategories(triggeredBy, includeInactiveCategories)
            .thenCompose(categoryLog -> {
                log.info("Categories sync completed, starting inventory items sync...");
                return syncInventoryItems(triggeredBy)
                    .thenApply(inventoryLog -> List.of(categoryLog, inventoryLog));
            })
            .exceptionally(error -> {
                log.error("Error during full MISA sync", error);
                throw new RuntimeException("Full sync failed", error);
            });
    }

    /**
     * Get sync statistics
     */
    public SyncStatistics getSyncStatistics() {
        Long totalItems = inventoryItemRepository.count();
        Long activeItems = inventoryItemRepository.countActiveItems();
        Long totalCategories = categoryRepository.count();
        Long activeCategories = categoryRepository.countActiveCategories();
        Long totalCustomers = customerRepository.count();
        Long activeCustomers = customerRepository.countActiveCustomers();
        Long totalInvoices = invoiceRepository.count();
        Long totalWarehouses = warehouseRepository.count();
        Long activeWarehouses = warehouseRepository.countByIsInactiveFalse();
        Long successfulSyncsToday = syncLogRepository.countSuccessfulSyncsSince(LocalDateTime.now().minusDays(1));
        Optional<MisaSyncLog> lastSync = getLastSuccessfulSync();

        return SyncStatistics.builder()
            .totalItems(totalItems)
            .activeItems(activeItems)
            .totalCategories(totalCategories)
            .activeCategories(activeCategories)
            .totalCustomers(totalCustomers)
            .activeCustomers(activeCustomers)
            .totalInvoices(totalInvoices)
            .totalWarehouses(totalWarehouses)
            .activeWarehouses(activeWarehouses)
            .successfulSyncsToday(successfulSyncsToday)
            .lastSyncTime(lastSync.map(MisaSyncLog::getCreatedAt).orElse(null))
            .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class SyncStatistics {
        private Long totalItems;
        private Long activeItems;
        private Long totalCategories;
        private Long activeCategories;
        private Long totalCustomers;
        private Long activeCustomers;
        private Long totalInvoices;
        private Long totalWarehouses;
        private Long activeWarehouses;
        private Long successfulSyncsToday;
        private LocalDateTime lastSyncTime;
    }

    /**
     * Synchronize customers from MISA API
     */
    @Async
    @Transactional
    public CompletableFuture<MisaSyncLog> syncCustomers(String triggeredBy) {
        log.info("Starting MISA customers sync triggered by: {}", triggeredBy);

        // Create sync log
        MisaSyncLog syncLog = new MisaSyncLog();
        syncLog.setSyncType(MisaSyncLog.SyncType.CUSTOMERS);
        syncLog.setStatus(MisaSyncLog.SyncStatus.STARTED);
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setTriggeredBy(triggeredBy);
        syncLog = syncLogRepository.save(syncLog);

        final MisaSyncLog finalSyncLog = syncLog;

        int pageSize = Math.max(1, batchSize);

        return misaApiClient.getCustomers(1, pageSize, "Name", 1, null)
            .map(response -> processCustomersResponse(response, finalSyncLog, pageSize))
            .doOnError(error -> handleSyncError(finalSyncLog, error))
            .toFuture();
    }

    /**
     * Process the MISA customers response
     */
    private MisaSyncLog processCustomersResponse(MisaApiResponse<MisaCustomerDto> response, MisaSyncLog syncLog, int pageSize) {
        try {
            if (response.getSuccess() != null && response.getSuccess()) {
                syncLog.setStatus(MisaSyncLog.SyncStatus.IN_PROGRESS);
                syncLog.setTotalRecords(response.getTotal());
                syncLog = syncLogRepository.save(syncLog);

                InventorySyncCounters counters = syncCustomersAcrossPages(response, pageSize);

                syncLog.setProcessedRecords(counters.processedRecords);
                syncLog.setSuccessfulRecords(counters.successfulRecords);
                syncLog.setFailedRecords(counters.failedRecords);
                syncLog.markAsCompleted();

                log.info("MISA customers sync completed - Successful: {}, Failed: {} (processed {})",
                        counters.successfulRecords,
                        counters.failedRecords,
                        counters.processedRecords);

            } else {
                syncLog.markAsFailed("MISA API returned error: " + response.getErrorMessage());
                log.error("MISA customers sync failed: {}", response.getErrorMessage());
            }
        } catch (Exception e) {
            syncLog.markAsFailed("Unexpected error during sync: " + e.getMessage());
            log.error("Error processing MISA customers response", e);
        }

        return syncLogRepository.save(syncLog);
    }

    private InventorySyncCounters syncCustomersAcrossPages(MisaApiResponse<MisaCustomerDto> firstPageResponse, int pageSize) {
        int totalRecords = firstPageResponse.getTotal() != null ? firstPageResponse.getTotal() : 0;
        int successful = 0;
        int failed = 0;
        int processed = 0;

        int currentPage = 1;
        MisaApiResponse<MisaCustomerDto> currentResponse = firstPageResponse;

        while (currentResponse != null && currentResponse.getData() != null && !currentResponse.getData().isEmpty()) {
            List<MisaCustomerDto> customers = currentResponse.getData();

            for (MisaCustomerDto customerDto : customers) {
                try {
                    syncCustomer(customerDto);
                    successful++;
                } catch (Exception e) {
                    log.error("Error syncing customer: {}", customerDto.getCode(), e);
                    failed++;
                }
            }

            processed += customers.size();

            if (totalRecords > 0 && processed >= totalRecords) {
                break;
            }

            currentPage++;
            MisaApiResponse<MisaCustomerDto> nextResponse = fetchCustomerPage(currentPage, pageSize);

            if (nextResponse == null || nextResponse.getData() == null || nextResponse.getData().isEmpty()) {
                log.info("No additional customers returned after page {}", currentPage - 1);
                break;
            }

            currentResponse = nextResponse;
        }

        return new InventorySyncCounters(processed, successful, failed);
    }

    private MisaApiResponse<MisaCustomerDto> fetchCustomerPage(int page, int pageSize) {
        try {
            return misaApiClient.getCustomers(page, pageSize, "Name", 1, null).block();
        } catch (Exception e) {
            log.error("Failed to fetch MISA customer page {}", page, e);
            return null;
        }
    }

    /**
     * Sync individual customer
     */
    @Transactional
    public void syncCustomer(MisaCustomerDto customerDto) {
        try {
            Optional<MisaCustomer> existingCustomer = customerRepository.findByCustomerId(customerDto.getId());

            MisaCustomer customer;
            if (existingCustomer.isPresent()) {
                customer = existingCustomer.get();
                updateCustomerFromDto(customer, customerDto);
                customer.setSyncStatus(MisaCustomer.SyncStatus.UPDATED);
            } else {
                customer = createCustomerFromDto(customerDto);
                customer.setSyncStatus(MisaCustomer.SyncStatus.SYNCED);
            }

            customer.setLastSyncDate(LocalDateTime.now());
            customer.setSyncErrorMessage(null);

            customerRepository.save(customer);

        } catch (Exception e) {
            log.error("Error syncing customer: {}", customerDto.getCode(), e);

            customerRepository.findByCustomerId(customerDto.getId())
                .ifPresent(customer -> {
                    customer.setSyncStatus(MisaCustomer.SyncStatus.ERROR);
                    customer.setSyncErrorMessage(e.getMessage());
                    customerRepository.save(customer);
                });

            throw e;
        }
    }

    private MisaCustomer createCustomerFromDto(MisaCustomerDto dto) {
        MisaCustomer customer = new MisaCustomer();
        updateCustomerFromDto(customer, dto);
        return customer;
    }

    private void updateCustomerFromDto(MisaCustomer customer, MisaCustomerDto dto) {
        customer.setCustomerId(dto.getId());
        customer.setCustomerCode(dto.getCode());
        customer.setCustomerName(dto.getName());
        customer.setPhone(dto.getTel());
        customer.setNormalizedPhone(dto.getNormalizedTel());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddr());
        customer.setProvince(dto.getProvinceAddr());
        customer.setDistrict(dto.getDistrictAddr());
        customer.setCommune(dto.getCommuneAddr());
        customer.setGender(dto.getGender());
        customer.setBirthday(dto.getBirthday() != null ? dto.getBirthday().toLocalDate() : null);
        customer.setIdentifyNumber(dto.getIdentifyNumber());
        customer.setDescription(dto.getDescription());
        customer.setMembershipCode(dto.getMembershipCode());
        customer.setMemberLevelId(dto.getMemberLevelId());
        customer.setMemberLevelName(dto.getMemberLevelName());
        customer.setCustomerCategoryId(dto.getCustomerCategoryId());
        customer.setCustomerCategoryName(dto.getCustomerCategoryName());
        customer.setIsActive(true);
        customer.setMisaLastModified(dto.getLastSyncDate() != null ? dto.getLastSyncDate().toLocalDateTime() : null);
    }

    /**
     * Synchronize invoices from MISA API
     */
    @Async
    @Transactional
    public CompletableFuture<MisaSyncLog> syncInvoices(String triggeredBy, String fromDate, String toDate) {
        log.info("Starting MISA invoices sync triggered by: {}, date range: {} to {}", triggeredBy, fromDate, toDate);

        // Create sync log
        MisaSyncLog syncLog = new MisaSyncLog();
        syncLog.setSyncType(MisaSyncLog.SyncType.INVOICES);
        syncLog.setStatus(MisaSyncLog.SyncStatus.STARTED);
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setTriggeredBy(triggeredBy);
        syncLog = syncLogRepository.save(syncLog);

        final MisaSyncLog finalSyncLog = syncLog;

        int pageSize = Math.max(1, batchSize);

        return misaApiClient.getInvoices(1, pageSize, null, null, fromDate, toDate)
            .map(response -> processInvoicesResponse(response, finalSyncLog, pageSize, fromDate, toDate))
            .doOnError(error -> handleSyncError(finalSyncLog, error))
            .toFuture();
    }

    /**
     * Process the MISA invoices response
     */
    private MisaSyncLog processInvoicesResponse(MisaApiResponse<MisaInvoiceDto> response, MisaSyncLog syncLog,
                                                 int pageSize, String fromDate, String toDate) {
        try {
            if (response.getSuccess() != null && response.getSuccess()) {
                syncLog.setStatus(MisaSyncLog.SyncStatus.IN_PROGRESS);
                syncLog.setTotalRecords(response.getTotal());
                syncLog = syncLogRepository.save(syncLog);

                InventorySyncCounters counters = syncInvoicesAcrossPages(response, pageSize, fromDate, toDate);

                syncLog.setProcessedRecords(counters.processedRecords);
                syncLog.setSuccessfulRecords(counters.successfulRecords);
                syncLog.setFailedRecords(counters.failedRecords);
                syncLog.markAsCompleted();

                log.info("MISA invoices sync completed - Successful: {}, Failed: {} (processed {})",
                        counters.successfulRecords,
                        counters.failedRecords,
                        counters.processedRecords);

            } else {
                syncLog.markAsFailed("MISA API returned error: " + response.getErrorMessage());
                log.error("MISA invoices sync failed: {}", response.getErrorMessage());
            }
        } catch (Exception e) {
            syncLog.markAsFailed("Unexpected error during sync: " + e.getMessage());
            log.error("Error processing MISA invoices response", e);
        }

        return syncLogRepository.save(syncLog);
    }

    private InventorySyncCounters syncInvoicesAcrossPages(MisaApiResponse<MisaInvoiceDto> firstPageResponse,
                                                          int pageSize, String fromDate, String toDate) {
        int totalRecords = firstPageResponse.getTotal() != null ? firstPageResponse.getTotal() : 0;
        int successful = 0;
        int failed = 0;
        int processed = 0;

        int currentPage = 1;
        MisaApiResponse<MisaInvoiceDto> currentResponse = firstPageResponse;

        while (currentResponse != null && currentResponse.getData() != null && !currentResponse.getData().isEmpty()) {
            List<MisaInvoiceDto> invoices = currentResponse.getData();

            for (MisaInvoiceDto invoiceDto : invoices) {
                try {
                    syncInvoice(invoiceDto);
                    successful++;
                } catch (Exception e) {
                    log.error("Error syncing invoice: {}", invoiceDto.getCode(), e);
                    failed++;
                }
            }

            processed += invoices.size();

            if (totalRecords > 0 && processed >= totalRecords) {
                break;
            }

            currentPage++;
            MisaApiResponse<MisaInvoiceDto> nextResponse = fetchInvoicePage(currentPage, pageSize, fromDate, toDate);

            if (nextResponse == null || nextResponse.getData() == null || nextResponse.getData().isEmpty()) {
                log.info("No additional invoices returned after page {}", currentPage - 1);
                break;
            }

            currentResponse = nextResponse;
        }

        return new InventorySyncCounters(processed, successful, failed);
    }

    private MisaApiResponse<MisaInvoiceDto> fetchInvoicePage(int page, int pageSize, String fromDate, String toDate) {
        try {
            return misaApiClient.getInvoices(page, pageSize, null, null, fromDate, toDate).block();
        } catch (Exception e) {
            log.error("Failed to fetch MISA invoice page {}", page, e);
            return null;
        }
    }

    /**
     * Sync individual invoice
     */
    @Transactional
    public void syncInvoice(MisaInvoiceDto invoiceDto) {
        try {
            // Log the incoming DTO to debug field mapping
            log.info("Syncing invoice - Id: {}, Code: {}, CustomerName: {}, Date: {}",
                    invoiceDto.getId(), invoiceDto.getCode(),
                    invoiceDto.getCustomerName(), invoiceDto.getInvoiceDate());

            if (invoiceDto.getId() == null) {
                log.error("Invoice ID is null! Full DTO: {}", invoiceDto);
                throw new RuntimeException("Invoice ID cannot be null");
            }

            Optional<MisaInvoice> existingInvoice = invoiceRepository.findByInvoiceId(invoiceDto.getId());

            MisaInvoice invoice;
            if (existingInvoice.isPresent()) {
                invoice = existingInvoice.get();
                updateInvoiceFromDto(invoice, invoiceDto);
                invoice.setSyncStatus(MisaInvoice.SyncStatus.UPDATED);
            } else {
                invoice = createInvoiceFromDto(invoiceDto);
                invoice.setSyncStatus(MisaInvoice.SyncStatus.SYNCED);
            }

            invoice.setLastSyncDate(LocalDateTime.now());
            invoice.setSyncErrorMessage(null);

            invoiceRepository.save(invoice);

        } catch (Exception e) {
            log.error("Error syncing invoice: {}", invoiceDto.getCode(), e);

            invoiceRepository.findByInvoiceId(invoiceDto.getId())
                .ifPresent(invoice -> {
                    invoice.setSyncStatus(MisaInvoice.SyncStatus.ERROR);
                    invoice.setSyncErrorMessage(e.getMessage());
                    invoiceRepository.save(invoice);
                });

            throw e;
        }
    }

    /**
     * Find customer ID by matching name or phone in our customers table
     */
    private String findCustomerIdByNameOrPhone(String customerName, String customerPhone) {
        if (customerName != null && !customerName.trim().isEmpty()) {
            // First try to find by exact name match
            Optional<MisaCustomer> customer = customerRepository.findByCustomerNameContainingOrPhoneContaining(
                customerName.trim(), customerPhone != null ? customerPhone.trim() : ""
            ).stream().findFirst();

            if (customer.isPresent()) {
                log.debug("Found customer match: {} -> {}", customerName, customer.get().getCustomerId());
                return customer.get().getCustomerId();
            }
        }

        log.debug("No customer found for name: {}, phone: {}", customerName, customerPhone);
        return null;
    }

    private MisaInvoice createInvoiceFromDto(MisaInvoiceDto dto) {
        MisaInvoice invoice = new MisaInvoice();
        updateInvoiceFromDto(invoice, dto);
        return invoice;
    }

    private void updateInvoiceFromDto(MisaInvoice invoice, MisaInvoiceDto dto) {
        invoice.setInvoiceId(dto.getId());
        invoice.setInvoiceCode(dto.getCode());
        invoice.setRefNo(null); // Not in API response

        // Use CustomerID from MISA API response if available, otherwise try to find by name/phone
        String customerId = dto.getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            customerId = findCustomerIdByNameOrPhone(dto.getCustomerName(), dto.getCustomerPhone());
        }
        invoice.setCustomerId(customerId);

        invoice.setCustomerCode(null); // Not in API response
        invoice.setCustomerName(dto.getCustomerName());
        invoice.setCustomerAddress(dto.getCustomerAddress());
        invoice.setCustomerPhone(dto.getCustomerPhone());
        invoice.setBranchId(dto.getBranchId());
        invoice.setBranchName(dto.getBranchName());
        invoice.setInvoiceDate(dto.getInvoiceDate() != null ? dto.getInvoiceDate().toLocalDateTime() : null);
        invoice.setTotalAmount(dto.getTotalAmount());
        invoice.setDiscountAmount(dto.getDiscountAmount());
        invoice.setTaxAmount(dto.getTaxAmount());
        invoice.setFinalAmount(dto.getFinalAmount());
        invoice.setPaidAmount(null); // Not in API response
        invoice.setDebtAmount(dto.getDebtAmount());
        invoice.setCashAmount(dto.getCashAmount());
        invoice.setCardAmount(dto.getCardAmount());
        invoice.setVoucherAmount(dto.getVoucherAmount());
        invoice.setStatus(null); // Not in API response
        invoice.setStatusName(null); // Not in API response
        invoice.setPaymentStatus(dto.getPaymentStatus());
        invoice.setPaymentStatusName(null); // Not in API response
        invoice.setDescription(dto.getDescription());
        invoice.setNote(dto.getNote());
        invoice.setCreatedBy(dto.getCashier()); // Use Cashier as created by
        invoice.setMisaCreatedDate(dto.getInvoiceDate() != null ? dto.getInvoiceDate().toLocalDateTime() : null);
        invoice.setMisaLastModified(null); // Not in API response
    }

    // ==================== WAREHOUSE SYNC (MISA AMIS API) ====================

    /**
     * Synchronize warehouses from MISA AMIS API
     * Extracts unique warehouses from inventory balance data since the dictionary API
     * returns accounts instead of warehouses
     */
    @Async
    @Transactional
    public CompletableFuture<MisaSyncLog> syncWarehouses(String triggeredBy) {
        log.info("Starting MISA AMIS warehouses sync triggered by: {}", triggeredBy);

        // Create sync log
        MisaSyncLog syncLog = new MisaSyncLog();
        syncLog.setSyncType(MisaSyncLog.SyncType.WAREHOUSES);
        syncLog.setStatus(MisaSyncLog.SyncStatus.STARTED);
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setTriggeredBy(triggeredBy);
        final MisaSyncLog savedSyncLog = syncLogRepository.save(syncLog);

        // Run synchronously in the async thread to avoid reactive blocking issues
        return CompletableFuture.supplyAsync(() -> processWarehousesSynchronously(savedSyncLog));
    }

    /**
     * Process warehouses synchronously (safe for blocking calls)
     */
    @Transactional
    public MisaSyncLog processWarehousesSynchronously(MisaSyncLog inputSyncLog) {
        // Refetch the sync log in the current thread's transaction
        MisaSyncLog syncLog = syncLogRepository.findById(inputSyncLog.getId())
            .orElse(inputSyncLog);

        try {
            syncLog.setStatus(MisaSyncLog.SyncStatus.IN_PROGRESS);
            syncLog = syncLogRepository.save(syncLog);

            // Fetch warehouses from MISA dictionary (data_type=5) — the authoritative source
            java.util.Map<String, WarehouseData> uniqueWarehouses = new java.util.HashMap<>();
            int skip = 0;
            boolean hasMore = true;

            while (hasMore) {
                JsonNode pageResponse = amisApiClient.getWarehouses(skip, 100).block();

                if (pageResponse != null && pageResponse.has("Success") &&
                    pageResponse.get("Success").asBoolean()) {

                    List<WarehouseData> pageWarehouses = extractWarehousesFromDictionary(pageResponse);

                    for (WarehouseData wd : pageWarehouses) {
                        if (wd.stockId != null && !wd.stockId.isEmpty()) {
                            uniqueWarehouses.putIfAbsent(wd.stockId, wd);
                        }
                    }

                    // Check if we have more pages
                    if (pageWarehouses.isEmpty()) {
                        hasMore = false;
                    } else {
                        skip += 100;
                        // Safety limit to prevent infinite loops
                        if (skip > 10000) {
                            log.warn("Reached safety limit of 10000 records, stopping pagination");
                            hasMore = false;
                        }
                    }
                } else {
                    if (pageResponse != null && pageResponse.has("ErrorMessage")) {
                        String errorMsg = pageResponse.get("ErrorMessage").asText();
                        log.error("MISA AMIS API error: {}", errorMsg);
                    }
                    hasMore = false;
                }
            }

            log.info("Found {} warehouses from MISA dictionary", uniqueWarehouses.size());
            syncLog.setTotalRecords(uniqueWarehouses.size());

            int successful = 0;
            int failed = 0;

            for (WarehouseData warehouseData : uniqueWarehouses.values()) {
                try {
                    syncWarehouse(warehouseData);
                    successful++;
                } catch (Exception e) {
                    log.error("Error syncing warehouse: {}", warehouseData.stockCode, e);
                    failed++;
                }
            }

            syncLog.setProcessedRecords(successful + failed);
            syncLog.setSuccessfulRecords(successful);
            syncLog.setFailedRecords(failed);
            syncLog.markAsCompleted();

            log.info("MISA AMIS warehouses sync completed - Successful: {}, Failed: {}", successful, failed);

        } catch (Exception e) {
            syncLog.markAsFailed("Unexpected error during warehouse sync: " + e.getMessage());
            log.error("Error processing MISA AMIS warehouses response", e);
        }

        return syncLogRepository.save(syncLog);
    }

    /**
     * Extract warehouse data from inventory balance response
     */
    private List<WarehouseData> extractWarehousesFromInventoryBalance(JsonNode response) {
        List<WarehouseData> warehouses = new java.util.ArrayList<>();

        try {
            if (response.has("Data")) {
                JsonNode dataNode = response.get("Data");
                String dataJson;

                if (dataNode.isTextual()) {
                    dataJson = dataNode.asText();
                } else {
                    dataJson = dataNode.toString();
                }

                if (dataJson != null && !dataJson.isEmpty() && !dataJson.equals("null")) {
                    List<java.util.Map<String, Object>> items = objectMapper.readValue(
                        dataJson,
                        new TypeReference<List<java.util.Map<String, Object>>>() {}
                    );

                    for (java.util.Map<String, Object> item : items) {
                        WarehouseData wd = new WarehouseData();
                        wd.stockId = getStringValue(item, "stock_id");
                        wd.stockCode = getStringValue(item, "stock_code");
                        wd.stockName = getStringValue(item, "stock_name");
                        // Branch info from inventory balance
                        wd.branchId = getStringValue(item, "organization_unit_id");
                        wd.branchCode = getStringValue(item, "organization_unit_code");
                        wd.branchName = getStringValue(item, "organization_unit_name");
                        wd.isInactive = false; // Assume active if in inventory
                        warehouses.add(wd);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting warehouse data from inventory balance: {}", e.getMessage());
        }

        return warehouses;
    }

    /**
     * Extract warehouse data from getDictionaryData(5) response.
     * Dictionary fields: stock_id, stock_code, stock_name, branch_id, inactive
     */
    private List<WarehouseData> extractWarehousesFromDictionary(JsonNode response) {
        List<WarehouseData> warehouses = new java.util.ArrayList<>();

        try {
            if (response.has("Data")) {
                JsonNode dataNode = response.get("Data");
                String dataJson;

                if (dataNode.isTextual()) {
                    dataJson = dataNode.asText();
                } else {
                    dataJson = dataNode.toString();
                }

                if (dataJson != null && !dataJson.isEmpty() && !dataJson.equals("null")) {
                    List<java.util.Map<String, Object>> items = objectMapper.readValue(
                        dataJson,
                        new TypeReference<List<java.util.Map<String, Object>>>() {}
                    );

                    for (java.util.Map<String, Object> item : items) {
                        WarehouseData wd = new WarehouseData();
                        wd.stockId = getStringValue(item, "stock_id");
                        wd.stockCode = getStringValue(item, "stock_code");
                        wd.stockName = getStringValue(item, "stock_name");
                        wd.branchId = getStringValue(item, "branch_id");
                        Object inactiveVal = item.get("inactive");
                        wd.isInactive = inactiveVal instanceof Boolean ? (Boolean) inactiveVal : false;
                        warehouses.add(wd);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting warehouse data from dictionary: {}", e.getMessage());
        }

        return warehouses;
    }

    private String getStringValue(java.util.Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Boolean getBooleanValue(java.util.Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Internal DTO for parsed warehouse data
     */
    private static class WarehouseData {
        String stockId;
        String stockCode;
        String stockName;
        String description;
        String address;
        String branchId;
        String branchCode;
        String branchName;
        Boolean isInactive;
    }

    /**
     * Sync individual warehouse
     */
    @Transactional
    public void syncWarehouse(WarehouseData data) {
        if (data.stockId == null || data.stockId.isEmpty()) {
            log.warn("Skipping warehouse with null/empty stock_id");
            return;
        }

        try {
            Optional<MisaWarehouse> existing = warehouseRepository.findByStockId(data.stockId);

            MisaWarehouse warehouse;
            if (existing.isPresent()) {
                warehouse = existing.get();
                updateWarehouseFromData(warehouse, data);
                warehouse.setSyncStatus(MisaWarehouse.SyncStatus.UPDATED);
            } else {
                // Also check by code in case ID changed
                Optional<MisaWarehouse> byCode = warehouseRepository.findByStockCode(data.stockCode);
                if (byCode.isPresent()) {
                    warehouse = byCode.get();
                    warehouse.setStockId(data.stockId);
                    updateWarehouseFromData(warehouse, data);
                    warehouse.setSyncStatus(MisaWarehouse.SyncStatus.UPDATED);
                } else {
                    warehouse = createWarehouseFromData(data);
                    warehouse.setSyncStatus(MisaWarehouse.SyncStatus.SYNCED);
                }
            }

            warehouse.setLastSyncDate(LocalDateTime.now());
            warehouse.setSyncErrorMessage(null);

            warehouseRepository.save(warehouse);

            log.debug("Synced warehouse: {} ({})", warehouse.getStockName(), warehouse.getStockCode());

        } catch (Exception e) {
            log.error("Error syncing warehouse: {}", data.stockCode, e);

            // Try to update error status if warehouse exists
            warehouseRepository.findByStockId(data.stockId)
                .ifPresent(wh -> {
                    wh.setSyncStatus(MisaWarehouse.SyncStatus.ERROR);
                    wh.setSyncErrorMessage(e.getMessage());
                    warehouseRepository.save(wh);
                });

            throw e;
        }
    }

    private MisaWarehouse createWarehouseFromData(WarehouseData data) {
        MisaWarehouse warehouse = new MisaWarehouse();
        updateWarehouseFromData(warehouse, data);
        return warehouse;
    }

    private void updateWarehouseFromData(MisaWarehouse warehouse, WarehouseData data) {
        warehouse.setStockId(data.stockId);
        warehouse.setStockCode(data.stockCode);
        warehouse.setStockName(data.stockName != null ? data.stockName : data.stockCode);
        warehouse.setDescription(data.description);
        warehouse.setAddress(data.address);
        warehouse.setBranchId(data.branchId);
        warehouse.setBranchCode(data.branchCode);
        warehouse.setBranchName(data.branchName);
        warehouse.setIsInactive(data.isInactive != null ? data.isInactive : false);
    }

    /**
     * Get all active warehouses
     */
    public List<MisaWarehouse> getActiveWarehouses() {
        return warehouseRepository.findByIsInactiveFalse();
    }

    /**
     * Get warehouse by stock code
     */
    public Optional<MisaWarehouse> getWarehouseByCode(String stockCode) {
        return warehouseRepository.findByStockCode(stockCode);
    }

    /**
     * Get warehouse by stock ID
     */
    public Optional<MisaWarehouse> getWarehouseById(String stockId) {
        return warehouseRepository.findByStockId(stockId);
    }

    /**
     * Search warehouses by name or code
     */
    public List<MisaWarehouse> searchWarehouses(String query) {
        return warehouseRepository.searchByNameOrCode(query);
    }
}
