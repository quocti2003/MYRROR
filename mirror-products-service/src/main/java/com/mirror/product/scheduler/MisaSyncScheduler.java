package com.mirror.product.scheduler;

import com.mirror.product.entity.misa.MisaSyncLog;
import com.mirror.product.service.MisaOrderIntegrationService;
import com.mirror.product.service.MisaPollingService;
import com.mirror.product.service.misa.MisaInventoryBalanceService;
import com.mirror.product.service.notification.MisaSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MisaSyncScheduler {

    private final MisaSyncService misaSyncService;
    private final MisaPollingService misaPollingService;
    private final MisaOrderIntegrationService misaOrderIntegrationService;
    private final MisaInventoryBalanceService misaInventoryBalanceService;

    /**
     * Daily inventory sync at 2 AM
     * Directly calls the local MisaSyncService to fetch from MISA API and update database
     */
    @Scheduled(cron = "${mirror.sync.inventory.cron:0 0/5 * * * ?}")
    public void scheduledInventorySync() {
        log.info("Starting scheduled MISA inventory sync...");

        try {
            CompletableFuture<MisaSyncLog> syncFuture = misaSyncService.syncInventoryItems("SCHEDULER");

            syncFuture.whenComplete((syncLog, error) -> {
                if (error != null) {
                    log.error("MISA inventory sync failed", error);
                } else {
                    log.info("MISA inventory sync completed successfully. Synced: {} items, Failed: {}",
                             syncLog.getSuccessfulRecords(), syncLog.getFailedRecords());
                }
            });
        } catch (Exception e) {
            log.error("Error starting MISA inventory sync", e);
        }
    }

    /**
     * Weekly category sync every Sunday at 3 AM
     * Directly calls the local MisaSyncService to fetch from MISA API and update database
     */
    @Scheduled(cron = "${mirror.sync.categories.cron:0 0 3 * * SUN}")
    public void scheduledCategorySync() {
        log.info("Starting scheduled MISA category sync...");

        try {
            CompletableFuture<MisaSyncLog> syncFuture = misaSyncService.syncProductCategories("SCHEDULER", false);

            syncFuture.whenComplete((syncLog, error) -> {
                if (error != null) {
                    log.error("MISA category sync failed", error);
                } else {
                    log.info("MISA category sync completed successfully. Synced: {} categories, Failed: {}",
                             syncLog.getSuccessfulRecords(), syncLog.getFailedRecords());
                }
            });
        } catch (Exception e) {
            log.error("Error starting MISA category sync", e);
        }
    }

    /**
     * Daily customer sync at 3 AM
     * Directly calls the local MisaSyncService to fetch from MISA API and update database
     */
    @Scheduled(cron = "${mirror.sync.customers.cron:0 0 3 * * ?}")
    public void scheduledCustomerSync() {
        log.info("Starting scheduled MISA customer sync...");

        try {
            CompletableFuture<MisaSyncLog> syncFuture = misaSyncService.syncCustomers("SCHEDULER");

            syncFuture.whenComplete((syncLog, error) -> {
                if (error != null) {
                    log.error("MISA customer sync failed", error);
                } else {
                    log.info("MISA customer sync completed successfully. Synced: {} customers, Failed: {}",
                             syncLog.getSuccessfulRecords(), syncLog.getFailedRecords());
                }
            });
        } catch (Exception e) {
            log.error("Error starting MISA customer sync", e);
        }
    }

    /**
     * Daily invoice sync at 4 AM
     * Syncs invoices from the last 30 days
     * Directly calls the local MisaSyncService to fetch from MISA API and update database
     */
    @Scheduled(cron = "${mirror.sync.invoices.cron:0 0 4 * * ?}")
    public void scheduledInvoiceSync() {
        log.info("Starting scheduled MISA invoice sync...");

        try {
            // Sync invoices from last 30 days
            String toDate = java.time.OffsetDateTime.now().format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String fromDate = java.time.OffsetDateTime.now().minusDays(30).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            CompletableFuture<MisaSyncLog> syncFuture = misaSyncService.syncInvoices("SCHEDULER", fromDate, toDate);

            syncFuture.whenComplete((syncLog, error) -> {
                if (error != null) {
                    log.error("MISA invoice sync failed", error);
                } else {
                    log.info("MISA invoice sync completed successfully. Synced: {} invoices, Failed: {}",
                             syncLog.getSuccessfulRecords(), syncLog.getFailedRecords());
                }
            });
        } catch (Exception e) {
            log.error("Error starting MISA invoice sync", e);
        }
    }

    /**
     * Warehouse sync every 6 hours
     * Syncs warehouse data from MISA AMIS API
     */
    @Scheduled(cron = "${mirror.sync.warehouses.cron:0 0 0/6 * * ?}")
    public void scheduledWarehouseSync() {
        log.info("Starting scheduled MISA AMIS warehouse sync...");

        try {
            CompletableFuture<MisaSyncLog> syncFuture = misaSyncService.syncWarehouses("SCHEDULER");

            syncFuture.whenComplete((syncLog, error) -> {
                if (error != null) {
                    log.error("MISA warehouse sync failed", error);
                } else {
                    log.info("MISA warehouse sync completed successfully. Synced: {} warehouses, Failed: {}",
                             syncLog.getSuccessfulRecords(), syncLog.getFailedRecords());
                }
            });
        } catch (Exception e) {
            log.error("Error starting MISA warehouse sync", e);
        }
    }

    /**
     * Full sync (categories + inventory) - Monthly on 1st at 1 AM
     * Directly calls the local MisaSyncService to fetch from MISA API and update database
     */
    @Scheduled(cron = "${mirror.sync.full.cron:0 0 1 1 * ?}")
    public void scheduledFullSync() {
        log.info("Starting scheduled MISA full sync (categories + inventory)...");

        try {
            CompletableFuture<List<MisaSyncLog>> syncFuture = misaSyncService.syncAllData("SCHEDULER", false);

            syncFuture.whenComplete((syncLogs, error) -> {
                if (error != null) {
                    log.error("MISA full sync failed", error);
                } else {
                    int totalSynced = syncLogs.stream()
                                              .mapToInt(MisaSyncLog::getSuccessfulRecords)
                                              .sum();
                    int totalFailed = syncLogs.stream()
                                              .mapToInt(MisaSyncLog::getFailedRecords)
                                              .sum();
                    log.info("MISA full sync completed successfully. Total synced: {}, Total failed: {}",
                             totalSynced, totalFailed);
                }
            });
        } catch (Exception e) {
            log.error("Error starting MISA full sync", e);
        }
    }

    /**
     * Poll MISA for callback results every 5 minutes.
     * Checks if any submitted SKU creations or invoices have been processed.
     */
    @Scheduled(cron = "${mirror.sync.order-polling.cron:0 0/5 * * * ?}")
    public void scheduledOrderPolling() {
        log.debug("Starting MISA order callback polling...");
        try {
            misaPollingService.pollCallbackResults();
        } catch (Exception e) {
            log.error("Error during MISA order callback polling", e);
        }
    }

    /**
     * Inventory balance sync from MISA AMIS every 5 minutes.
     * Polls AMIS for current inventory balances and propagates to Sku/MirrorProduct.
     */
    @Scheduled(cron = "${mirror.sync.inventory-balance.cron:0 0/5 * * * ?}")
    public void scheduledInventoryBalanceSync() {
        log.info("Starting scheduled MISA inventory balance sync...");

        try {
            CompletableFuture<MisaSyncLog> syncFuture = misaInventoryBalanceService.syncInventoryBalance("SCHEDULER");

            syncFuture.whenComplete((syncLog, error) -> {
                if (error != null) {
                    log.error("MISA inventory balance sync failed", error);
                } else {
                    log.info("MISA inventory balance sync completed. Updated: {}, Failed: {}",
                             syncLog.getSuccessfulRecords(), syncLog.getFailedRecords());
                }
            });
        } catch (Exception e) {
            log.error("Error starting MISA inventory balance sync", e);
        }
    }

    /**
     * Retry failed order MISA submissions every 30 minutes.
     * Retries both SKU creation and invoice submission for orders that failed.
     */
    @Scheduled(cron = "${mirror.sync.order-retry.cron:0 0/30 * * * ?}")
    public void scheduledOrderRetry() {
        log.info("Starting MISA order retry...");
        try {
            misaOrderIntegrationService.retryFailedSkuCreations();
            misaOrderIntegrationService.retryFailedInvoiceSubmissions();
        } catch (Exception e) {
            log.error("Error during MISA order retry", e);
        }
    }
}
