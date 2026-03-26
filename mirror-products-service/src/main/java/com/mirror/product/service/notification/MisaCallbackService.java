package com.mirror.product.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.config.misa.MisaConfig;
import com.mirror.product.dto.notification.MisaCallbackRequest;
import com.mirror.product.dto.notification.MisaCallbackResponse;
import com.mirror.product.entity.misa.MisaCallbackLog;
import com.mirror.product.repository.misa.MisaCallbackLogRepository;
import com.mirror.product.repository.misa.MisaInventoryItemRepository;
import com.mirror.product.util.MisaSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling MISA AMIS Open API callbacks
 *
 * This service receives asynchronous callbacks from MISA after voucher/dictionary
 * operations complete. It validates signatures, logs callbacks, and processes
 * the results to update local data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MisaCallbackService {

    private final MisaCallbackLogRepository callbackLogRepository;
    private final MisaInventoryItemRepository inventoryItemRepository;
    private final MisaSignatureVerifier signatureVerifier;
    private final MisaConfig.MisaApiProperties misaApiProperties;
    private final ObjectMapper objectMapper;

    /**
     * Process a callback from MISA
     *
     * @param request The callback request
     * @param rawPayload The raw JSON payload for logging
     * @return Response to send back to MISA
     */
    @Transactional
    public MisaCallbackResponse processCallback(MisaCallbackRequest request, String rawPayload) {
        log.info("Processing MISA callback: dataType={}, orgRefId={}, appId={}",
                request.getDataType(),
                request.getData() != null ? request.getData().getOrgRefId() : null,
                request.getAppId());

        // Create log entry
        MisaCallbackLog logEntry = createLogEntry(request, rawPayload);

        try {
            // Validate app_id matches our configured app_id
            if (!validateAppId(request.getAppId())) {
                logEntry.setProcessingSuccess(false);
                logEntry.setProcessingError("Invalid app_id");
                callbackLogRepository.save(logEntry);
                return MisaCallbackResponse.error(
                        MisaCallbackResponse.ErrorCodes.INVALID_APP_ID,
                        "App ID does not match configured value"
                );
            }

            // Verify signature
            boolean signatureValid = verifySignature(request, rawPayload);
            logEntry.setSignatureValid(signatureValid);

            if (!signatureValid) {
                log.warn("Invalid signature for callback from MISA. AppId: {}", request.getAppId());
                logEntry.setProcessingSuccess(false);
                logEntry.setProcessingError("Invalid signature");
                callbackLogRepository.save(logEntry);
                // Still return success to MISA to avoid retries, but log the issue
                // In production, you may want to reject invalid signatures
                return MisaCallbackResponse.error(
                        MisaCallbackResponse.ErrorCodes.INVALID_SIGNATURE,
                        "Signature verification failed"
                );
            }

            // Process based on data type
            processCallbackData(request, logEntry);

            // Mark as processed
            logEntry.setProcessed(true);
            logEntry.setProcessingSuccess(true);
            logEntry.setProcessedAt(LocalDateTime.now());
            callbackLogRepository.save(logEntry);

            return MisaCallbackResponse.success();

        } catch (Exception e) {
            log.error("Error processing MISA callback", e);
            logEntry.setProcessed(true);
            logEntry.setProcessingSuccess(false);
            logEntry.setProcessingError(e.getMessage());
            logEntry.setProcessedAt(LocalDateTime.now());
            callbackLogRepository.save(logEntry);

            return MisaCallbackResponse.error(
                    MisaCallbackResponse.ErrorCodes.PROCESSING_ERROR,
                    "Internal processing error: " + e.getMessage()
            );
        }
    }

    /**
     * Create a log entry from the callback request
     */
    private MisaCallbackLog createLogEntry(MisaCallbackRequest request, String rawPayload) {
        MisaCallbackLog.MisaCallbackLogBuilder builder = MisaCallbackLog.builder()
                .appId(request.getAppId())
                .orgCompanyCode(request.getOrgCompanyCode())
                .dataType(request.getDataType())
                .signature(request.getSignature())
                .rawPayload(rawPayload)
                .receivedAt(LocalDateTime.now());

        if (request.getData() != null) {
            MisaCallbackRequest.CallbackData data = request.getData();
            builder.orgRefId(data.getOrgRefId())
                    .orgRefNo(data.getOrgRefNo())
                    .misaRefId(data.getRefId())
                    .misaRefNo(data.getRefNo())
                    .voucherType(data.getVoucherType())
                    .misaSuccess(data.getSuccess() != null ? data.getSuccess() : false)
                    .misaErrorCode(data.getErrorCode())
                    .misaErrorMessage(data.getErrorMessage());
        } else {
            builder.misaSuccess(false);
        }

        return builder.build();
    }

    /**
     * Validate that the app_id matches our configuration
     */
    private boolean validateAppId(String appId) {
        String configuredAppId = misaApiProperties.getAppId();
        if (configuredAppId == null || configuredAppId.isEmpty()) {
            log.warn("No app_id configured, skipping validation");
            return true; // Skip validation if not configured
        }
        return configuredAppId.equals(appId);
    }

    /**
     * Verify the signature of the callback request
     */
    private boolean verifySignature(MisaCallbackRequest request, String rawPayload) {
        String appId = misaApiProperties.getAppId();
        if (appId == null || appId.isEmpty()) {
            log.warn("No app_id configured for signature verification, skipping");
            return true;
        }

        // The signature key is the app_id according to MISA documentation
        return signatureVerifier.verifySignature(rawPayload, request.getSignature(), appId);
    }

    /**
     * Process the callback data based on data type
     */
    private void processCallbackData(MisaCallbackRequest request, MisaCallbackLog logEntry) {
        Integer dataType = request.getDataType();
        MisaCallbackRequest.CallbackData data = request.getData();

        if (data == null) {
            log.warn("Callback has no data payload");
            return;
        }

        switch (dataType) {
            case 1 -> handleVoucherSave(data, logEntry);
            case 2 -> handleVoucherDelete(data, logEntry);
            case 3 -> handleVoucherUpdate(data, logEntry);
            case 6 -> handleWarehouseExport(data, logEntry);
            case 7 -> handleDictionaryCreate(data, logEntry);
            case 8 -> handlePaymentRequest(data, logEntry);
            case 15 -> handlePaymentResponse(data, logEntry);
            default -> log.warn("Unknown data type: {}", dataType);
        }
    }

    /**
     * Handle voucher save callback (data_type = 1)
     */
    private void handleVoucherSave(MisaCallbackRequest.CallbackData data, MisaCallbackLog logEntry) {
        log.info("Handling voucher save callback: orgRefId={}, misaRefId={}, success={}",
                data.getOrgRefId(), data.getRefId(), data.getSuccess());

        if (Boolean.TRUE.equals(data.getSuccess())) {
            // Voucher was successfully created in MISA
            // Update local records with MISA reference ID
            updateLocalVoucherReference(data.getOrgRefId(), data.getRefId(), data.getRefNo());
        } else {
            // Voucher creation failed
            log.error("Voucher save failed in MISA: {} - {}",
                    data.getErrorCode(), data.getErrorMessage());
            // Could trigger notification or retry logic here
        }

        // Process detail items if present
        if (data.getDetail() != null && !data.getDetail().isEmpty()) {
            processDetailItems(data.getDetail(), logEntry);
        }
    }

    /**
     * Handle voucher delete callback (data_type = 2)
     */
    private void handleVoucherDelete(MisaCallbackRequest.CallbackData data, MisaCallbackLog logEntry) {
        log.info("Handling voucher delete callback: orgRefId={}, success={}",
                data.getOrgRefId(), data.getSuccess());

        if (Boolean.TRUE.equals(data.getSuccess())) {
            // Mark local voucher as deleted in MISA
            markVoucherDeleted(data.getOrgRefId());
        }
    }

    /**
     * Handle voucher update callback (data_type = 3)
     */
    private void handleVoucherUpdate(MisaCallbackRequest.CallbackData data, MisaCallbackLog logEntry) {
        log.info("Handling voucher update callback: orgRefId={}, success={}",
                data.getOrgRefId(), data.getSuccess());

        // Similar to save, update local references
        if (Boolean.TRUE.equals(data.getSuccess())) {
            updateLocalVoucherReference(data.getOrgRefId(), data.getRefId(), data.getRefNo());
        }
    }

    /**
     * Handle warehouse export callback (data_type = 6)
     */
    private void handleWarehouseExport(MisaCallbackRequest.CallbackData data, MisaCallbackLog logEntry) {
        log.info("Handling warehouse export callback: orgRefId={}, success={}",
                data.getOrgRefId(), data.getSuccess());

        // Process inventory changes from warehouse export
        if (data.getDetail() != null) {
            for (MisaCallbackRequest.DetailItem item : data.getDetail()) {
                log.debug("Warehouse export item: code={}, qty={}",
                        item.getInventoryItemCode(), item.getQuantity());
                // Update local inventory quantities if needed
            }
        }
    }

    /**
     * Handle dictionary create callback (data_type = 7)
     */
    private void handleDictionaryCreate(MisaCallbackRequest.CallbackData data, MisaCallbackLog logEntry) {
        log.info("Handling dictionary create callback: success={}", data.getSuccess());

        if (data.getDictionary() != null) {
            for (MisaCallbackRequest.DictionaryItem item : data.getDictionary()) {
                log.info("Dictionary item created: type={}, orgId={}, misaId={}, code={}, success={}",
                        item.getDictionaryType(), item.getOrgId(), item.getMisaId(),
                        item.getCode(), item.getSuccess());

                if (Boolean.TRUE.equals(item.getSuccess())) {
                    // Update local record with MISA ID
                    updateDictionaryMisaId(item.getDictionaryType(), item.getOrgId(), item.getMisaId());
                } else {
                    log.error("Dictionary item creation failed: {} - {}",
                            item.getErrorCode(), item.getErrorMessage());
                }
            }
        }
    }

    /**
     * Handle payment request callback (data_type = 8)
     */
    private void handlePaymentRequest(MisaCallbackRequest.CallbackData data, MisaCallbackLog logEntry) {
        log.info("Handling payment request callback: orgRefId={}", data.getOrgRefId());
        // Process payment status request from MISA
    }

    /**
     * Handle payment response callback (data_type = 15)
     */
    private void handlePaymentResponse(MisaCallbackRequest.CallbackData data, MisaCallbackLog logEntry) {
        log.info("Handling payment response callback: orgRefId={}, success={}",
                data.getOrgRefId(), data.getSuccess());
        // Process payment confirmation from MISA
    }

    /**
     * Update local voucher with MISA reference
     */
    private void updateLocalVoucherReference(String orgRefId, String misaRefId, String misaRefNo) {
        // TODO: Implement based on your voucher storage
        log.debug("Updating voucher reference: orgRefId={} -> misaRefId={}, misaRefNo={}",
                orgRefId, misaRefId, misaRefNo);
    }

    /**
     * Mark a voucher as deleted
     */
    private void markVoucherDeleted(String orgRefId) {
        // TODO: Implement based on your voucher storage
        log.debug("Marking voucher as deleted: orgRefId={}", orgRefId);
    }

    /**
     * Update dictionary item with MISA ID
     */
    private void updateDictionaryMisaId(Integer dictionaryType, String orgId, String misaId) {
        // Dictionary types: 1=Objects, 2=Groups, 3=Inventory, 4=Categories, etc.
        log.debug("Updating dictionary MISA ID: type={}, orgId={} -> misaId={}",
                dictionaryType, orgId, misaId);

        // Update inventory item with MISA-assigned ID
        if (dictionaryType != null && dictionaryType == 3) {
            // orgId is our internal code, misaId is the ID assigned by MISA
            inventoryItemRepository.findByInventoryItemCode(orgId).ifPresent(item -> {
                item.setInventoryItemId(misaId);
                inventoryItemRepository.save(item);
                log.info("Updated inventory item {} with MISA ID {}", orgId, misaId);
            });
        }
    }

    /**
     * Process detail items from callback
     */
    private void processDetailItems(List<MisaCallbackRequest.DetailItem> items, MisaCallbackLog logEntry) {
        for (MisaCallbackRequest.DetailItem item : items) {
            if (item.getErrorCode() != null) {
                log.warn("Detail item error: {} - {} for inventory {}",
                        item.getErrorCode(), item.getErrorMessage(), item.getInventoryItemCode());
            }
        }
    }

    /**
     * Get callback statistics
     */
    public CallbackStatistics getStatistics() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        long successful = callbackLogRepository.countByMisaSuccessTrueAndReceivedAtAfter(since);
        long failed = callbackLogRepository.countByMisaSuccessFalseAndReceivedAtAfter(since);
        List<MisaCallbackLog> recent = callbackLogRepository.findTop50ByOrderByReceivedAtDesc();

        return new CallbackStatistics(successful, failed, recent);
    }

    /**
     * Get unprocessed callbacks for retry
     */
    public List<MisaCallbackLog> getUnprocessedCallbacks() {
        return callbackLogRepository.findByProcessedFalseOrderByReceivedAtAsc();
    }

    /**
     * Statistics record
     */
    public record CallbackStatistics(long successfulCount, long failedCount, List<MisaCallbackLog> recentCallbacks) {}
}
