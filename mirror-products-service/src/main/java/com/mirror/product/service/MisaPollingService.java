package com.mirror.product.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.client.misa.MisaAmisApiClient;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.Order;
import com.mirror.product.entity.misa.MisaCallbackLog;
import com.mirror.product.entity.misa.MisaCustomer;
import com.mirror.product.entity.misa.MisaSyncLog;
import com.mirror.product.repository.MirrorProductRepository;
import com.mirror.product.repository.OrderRepository;
import com.mirror.product.repository.misa.MisaCallbackLogRepository;
import com.mirror.product.repository.misa.MisaCustomerRepository;
import com.mirror.product.repository.misa.MisaSyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MisaPollingService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int POLL_BATCH_SIZE = 100;
    private static final String ALREADY_EXISTS_ERROR = "Danh mục đã tồn tại";

    private final MisaAmisApiClient amisApiClient;
    private final OrderRepository orderRepository;
    private final MirrorProductRepository mirrorProductRepository;
    private final MisaCustomerRepository misaCustomerRepository;
    private final MisaCallbackLogRepository callbackLogRepository;
    private final MisaSyncLogRepository syncLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Poll MISA for callback results and process them.
     * Checks for both SKU creation results (data_type=7) and voucher results (data_type=1).
     */
    public void pollCallbackResults() {
        log.info("Starting MISA callback polling...");

        MisaSyncLog syncLog = new MisaSyncLog();
        syncLog.setSyncType(MisaSyncLog.SyncType.MISA_POLLING);
        syncLog.setStatus(MisaSyncLog.SyncStatus.STARTED);
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setTriggeredBy("SCHEDULER");
        syncLogRepository.save(syncLog);

        int totalProcessed = 0;
        int totalSuccess = 0;
        int totalFailed = 0;

        try {
            String fromDate = LocalDate.now().minusDays(7).format(DATE_FORMAT);
            String toDate = LocalDate.now().plusDays(1).format(DATE_FORMAT);

            int skip = 0;
            boolean hasMore = true;

            while (hasMore) {
                JsonNode response = amisApiClient.getCallbackDetailError(fromDate, toDate, skip, POLL_BATCH_SIZE).block();

                if (response == null || !response.has("Data")) {
                    log.info("No more callback data to process");
                    break;
                }

                // Data field can be a JSON string or a JSON array
                JsonNode dataNode = response.get("Data");
                JsonNode data;
                if (dataNode.isTextual()) {
                    try {
                        data = objectMapper.readTree(dataNode.asText());
                    } catch (Exception e) {
                        log.warn("Failed to parse Data string as JSON: {}", e.getMessage());
                        break;
                    }
                } else {
                    data = dataNode;
                }

                if (!data.isArray() || data.isEmpty()) {
                    break;
                }

                for (JsonNode entry : data) {
                    totalProcessed++;
                    try {
                        boolean processed = processCallbackEntry(entry);
                        if (processed) {
                            totalSuccess++;
                        }
                    } catch (Exception e) {
                        totalFailed++;
                        log.error("Error processing callback entry: {}", e.getMessage());
                    }
                }

                if (data.size() < POLL_BATCH_SIZE) {
                    hasMore = false;
                } else {
                    skip += POLL_BATCH_SIZE;
                }
            }

            syncLog.setProcessedRecords(totalProcessed);
            syncLog.setSuccessfulRecords(totalSuccess);
            syncLog.setFailedRecords(totalFailed);
            syncLog.markAsCompleted();
            syncLogRepository.save(syncLog);

            log.info("MISA polling completed - Processed: {}, Success: {}, Failed: {}",
                    totalProcessed, totalSuccess, totalFailed);

        } catch (Exception e) {
            log.error("MISA polling failed", e);
            syncLog.setProcessedRecords(totalProcessed);
            syncLog.setSuccessfulRecords(totalSuccess);
            syncLog.setFailedRecords(totalFailed);
            syncLog.markAsFailed(e.getMessage());
            syncLogRepository.save(syncLog);
        }
    }

    /**
     * Process a single callback entry from MISA.
     * Returns true if the entry matched an order and was processed.
     */
    @Transactional
    protected boolean processCallbackEntry(JsonNode entry) {
        int dataType = entry.has("data_type") ? entry.get("data_type").asInt() : -1;
        String orgRefId = entry.has("org_refid") ? entry.get("org_refid").asText() : null;
        // MISA uses "success" boolean field, convert to int (1=success, 0=failure)
        int status;
        if (entry.has("success")) {
            status = entry.get("success").asBoolean() ? 1 : 0;
        } else if (entry.has("status")) {
            status = entry.get("status").asInt();
        } else {
            status = -1;
        }
        String errorMessage = entry.has("error_message") ? entry.get("error_message").asText() : null;
        int voucherType = entry.has("voucher_type") ? entry.get("voucher_type").asInt() : -1;

        if (orgRefId == null || orgRefId.isBlank()) {
            return false;
        }

        // Log the callback
        saveCallbackLog(entry, orgRefId, dataType, status, errorMessage);

        // data_type=7 = dictionary/inventory items (SKU creation result)
        if (dataType == 7) {
            return processSkuCreationResult(orgRefId, status, errorMessage);
        }

        // data_type=1 with voucher_type=11 = sales invoice result
        if (dataType == 1 && voucherType == 11) {
            return processInvoiceResult(orgRefId, status, errorMessage);
        }

        return false;
    }

    /**
     * Process SKU creation result from MISA callback.
     * Success status=1 means the SKU was created successfully.
     * Also handles "Danh mục đã tồn tại" (already exists) as success.
     */
    private boolean processSkuCreationResult(String orgRefId, int status, String errorMessage) {
        // Treat "already exists" as success - the item is in MISA, no need to retry
        boolean isAlreadyExists = errorMessage != null && errorMessage.contains(ALREADY_EXISTS_ERROR);
        boolean isEffectiveSuccess = status == 1 || isAlreadyExists;

        // Try to find an Order first
        Optional<Order> orderOpt = orderRepository.findActiveById(orgRefId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (Boolean.TRUE.equals(order.getMisaItemCreated())) {
                return false; // Already processed
            }

            if (isEffectiveSuccess) {
                order.setMisaItemCreated(true);
                order.setMisaLastSyncAttempt(LocalDateTime.now());
                orderRepository.save(order);

                // Also mark the associated product as synced
                if (order.getProductId() != null) {
                    mirrorProductRepository.findById(order.getProductId()).ifPresent(product -> {
                        if (!Boolean.TRUE.equals(product.getMisaSynced())) {
                            product.setMisaSynced(true);
                            product.setMisaSyncDate(LocalDateTime.now());
                            if (product.getMisaItemCode() == null) {
                                product.setMisaItemCode(product.getDescriptiveCode() != null
                                        ? product.getDescriptiveCode() : product.getSkuCode());
                            }
                            mirrorProductRepository.save(product);
                            log.info("Also marked product {} as synced", product.getSkuCode());
                        }
                    });
                }

                log.info("MISA SKU creation confirmed for order: {} (alreadyExists={})", orgRefId, isAlreadyExists);
                return true;
            } else {
                log.warn("MISA SKU creation failed for order: {}, error: {}", orgRefId, errorMessage);
                order.setMisaLastSyncAttempt(LocalDateTime.now());
                orderRepository.save(order);
                return true;
            }
        }

        // No Order found - try to find a MirrorProduct by skuCode (used as org_refid when pushing products directly)
        Optional<MirrorProduct> productOpt = mirrorProductRepository.findBySkuCode(orgRefId);
        if (productOpt.isPresent()) {
            MirrorProduct product = productOpt.get();
            if (Boolean.TRUE.equals(product.getMisaSynced())) {
                return false; // Already synced
            }

            if (isEffectiveSuccess) {
                product.setMisaSynced(true);
                product.setMisaSyncDate(LocalDateTime.now());
                if (product.getMisaItemCode() == null) {
                    product.setMisaItemCode(product.getDescriptiveCode() != null
                            ? product.getDescriptiveCode() : product.getSkuCode());
                }
                mirrorProductRepository.save(product);
                log.info("MISA product sync confirmed for SKU: {} (alreadyExists={})", orgRefId, isAlreadyExists);
                return true;
            } else {
                log.warn("MISA product sync failed for SKU: {}, error: {}", orgRefId, errorMessage);
                return true;
            }
        }

        // Also try to find by descriptiveCode (some products may use descriptiveCode as org_refid)
        Optional<MirrorProduct> productByDescOpt = mirrorProductRepository.findByDescriptiveCode(orgRefId);
        if (productByDescOpt.isPresent()) {
            MirrorProduct product = productByDescOpt.get();
            if (Boolean.TRUE.equals(product.getMisaSynced())) {
                return false; // Already synced
            }

            if (isEffectiveSuccess) {
                product.setMisaSynced(true);
                product.setMisaSyncDate(LocalDateTime.now());
                if (product.getMisaItemCode() == null) {
                    product.setMisaItemCode(product.getDescriptiveCode());
                }
                mirrorProductRepository.save(product);
                log.info("MISA product sync confirmed for descriptiveCode: {} (alreadyExists={})", orgRefId, isAlreadyExists);
                return true;
            } else {
                log.warn("MISA product sync failed for descriptiveCode: {}, error: {}", orgRefId, errorMessage);
                return true;
            }
        }

        // Try to find a MisaCustomer by customerCode (for customer dictionary callbacks)
        Optional<MisaCustomer> customerOpt = misaCustomerRepository.findByCustomerCode(orgRefId);
        if (customerOpt.isPresent()) {
            MisaCustomer customer = customerOpt.get();
            if (customer.getSyncStatus() == MisaCustomer.SyncStatus.SYNCED) {
                return false; // Already synced
            }

            if (isEffectiveSuccess) {
                customer.setSyncStatus(MisaCustomer.SyncStatus.SYNCED);
                customer.setLastSyncDate(LocalDateTime.now());
                customer.setSyncErrorMessage(null);
                misaCustomerRepository.save(customer);
                log.info("MISA customer sync confirmed for code: {} (alreadyExists={})", orgRefId, isAlreadyExists);
                return true;
            } else {
                customer.setSyncStatus(MisaCustomer.SyncStatus.ERROR);
                customer.setSyncErrorMessage(errorMessage);
                misaCustomerRepository.save(customer);
                log.warn("MISA customer sync failed for code: {}, error: {}", orgRefId, errorMessage);
                return true;
            }
        }

        log.debug("No order/product/customer found for callback with org_refid: {}", orgRefId);
        return false;
    }

    /**
     * Process sales invoice result from MISA callback.
     * Success status=1 means the invoice was recorded successfully.
     * Also handles "Danh mục đã tồn tại" (already exists) as success.
     */
    private boolean processInvoiceResult(String orgRefId, int status, String errorMessage) {
        // Treat "already exists" as success
        boolean isAlreadyExists = errorMessage != null && errorMessage.contains(ALREADY_EXISTS_ERROR);
        boolean isEffectiveSuccess = status == 1 || isAlreadyExists;

        Optional<Order> orderOpt = orderRepository.findActiveById(orgRefId);
        if (orderOpt.isEmpty()) {
            log.debug("No order found for invoice callback with org_refid: {}", orgRefId);
            return false;
        }

        Order order = orderOpt.get();
        if (Boolean.TRUE.equals(order.getMisaSaleRecorded())) {
            return false; // Already processed
        }

        if (isEffectiveSuccess) {
            order.setMisaSaleRecorded(true);
            order.setMisaLastSyncAttempt(LocalDateTime.now());
            orderRepository.save(order);
            log.info("MISA sale recorded confirmed for order: {} (alreadyExists={})", orgRefId, isAlreadyExists);
            return true;
        } else {
            log.warn("MISA invoice submission failed for order: {}, error: {}", orgRefId, errorMessage);
            order.setMisaLastSyncAttempt(LocalDateTime.now());
            orderRepository.save(order);
            return true;
        }
    }

    private void saveCallbackLog(JsonNode entry, String orgRefId, int dataType, int status, String errorMessage) {
        try {
            MisaCallbackLog callbackLog = MisaCallbackLog.builder()
                    .appId(amisApiClient.getProperties().getAppId())
                    .orgCompanyCode(amisApiClient.getProperties().getOrgCompanyCode())
                    .dataType(dataType)
                    .orgRefId(orgRefId)
                    .orgRefNo(entry.has("org_refno") ? entry.get("org_refno").asText() : null)
                    .misaRefId(entry.has("misa_refid") ? entry.get("misa_refid").asText() : null)
                    .voucherType(entry.has("voucher_type") ? entry.get("voucher_type").asInt() : null)
                    .misaSuccess(status == 1)
                    .misaErrorMessage(errorMessage)
                    .processed(true)
                    .processingSuccess(true)
                    .rawPayload(entry.toString())
                    .receivedAt(LocalDateTime.now())
                    .processedAt(LocalDateTime.now())
                    .build();
            callbackLogRepository.save(callbackLog);
        } catch (Exception e) {
            log.warn("Failed to save callback log for org_refid: {}", orgRefId, e);
        }
    }
}
