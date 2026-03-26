package com.mirror.product.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.dto.FileUploadResponse;
import com.mirror.product.dto.StockReconciliationDTO;
import com.mirror.product.dto.StockReconciliationDTO.*;
import com.mirror.product.entity.StockReconciliationRecord;
import com.mirror.product.repository.StockReconciliationRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StockReconciliationService {

    private final StockReconciliationRecordRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private R2Service r2Service;

    public StockReconciliationService(
            StockReconciliationRecordRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Create a new stock reconciliation record with file upload
     */
    @Transactional
    public CreateResponse createRecord(CreateRequest request, byte[] reportFileContent,
                                       String userId, String userName) {
        log.info("Creating stock reconciliation record for warehouse: {}", request.getWarehouseName());

        // Generate document number if not provided
        String documentNumber = request.getDocumentNumber();
        if (documentNumber == null || documentNumber.isEmpty()) {
            documentNumber = generateDocumentNumber();
        }

        // Check for duplicate document number
        if (repository.existsByDocumentNumberAndIsDeletedFalse(documentNumber)) {
            throw new IllegalArgumentException("Document number already exists: " + documentNumber);
        }

        // Upload file to R2 if available
        String reportFileKey = null;
        String reportFileUrl = null;
        String reportFileName = generateFileName(request.getWarehouseName(), request.getReconciliationDate());

        if (r2Service != null && reportFileContent != null && reportFileContent.length > 0) {
            try {
                String folderPath = generateFolderPath(request.getReconciliationDate());
                reportFileKey = folderPath + documentNumber + ".docx";

                FileUploadResponse uploadResponse = r2Service.uploadFile(
                        reportFileKey,
                        new ByteArrayInputStream(reportFileContent),
                        reportFileContent.length,
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                );

                reportFileUrl = uploadResponse.getPublicUrl();
                log.info("Uploaded reconciliation report to R2: {}", reportFileKey);
            } catch (Exception e) {
                log.error("Failed to upload report to R2, continuing without file storage: {}", e.getMessage());
                // Don't fail the whole operation if file upload fails
            }
        } else {
            log.warn("R2Service not available or no file content provided, skipping file upload");
        }

        // Calculate statistics from items
        Summary summary = request.getSummary();
        int totalItems = request.getItems() != null ? request.getItems().size() : 0;
        int matchedItems = summary != null ? (summary.getMatch() != null ? summary.getMatch() : 0) : 0;
        int missingItems = summary != null ? (summary.getMissing() != null ? summary.getMissing() : 0) : 0;
        int excessItems = summary != null ? (summary.getExcess() != null ? summary.getExcess() : 0) : 0;
        int notInSystemItems = summary != null ? (summary.getNotInMisa() != null ? summary.getNotInMisa() : 0) : 0;

        // Convert summary to Map for JSON storage
        Map<String, Object> summaryMap = new HashMap<>();
        if (summary != null) {
            summaryMap.put("total", summary.getTotal());
            summaryMap.put("match", summary.getMatch());
            summaryMap.put("missing", summary.getMissing());
            summaryMap.put("excess", summary.getExcess());
            summaryMap.put("notInMisa", summary.getNotInMisa());
        }

        // Serialize items to JSON string
        String itemsDataJson = null;
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            try {
                itemsDataJson = objectMapper.writeValueAsString(request.getItems());
            } catch (Exception e) {
                log.warn("Failed to serialize items data: {}", e.getMessage());
            }
        }

        // Build entity
        StockReconciliationRecord record = StockReconciliationRecord.builder()
                .documentNumber(documentNumber)
                .warehouseId(request.getWarehouseId())
                .warehouseName(request.getWarehouseName())
                .reconciliationDate(request.getReconciliationDate() != null ?
                        request.getReconciliationDate() : LocalDateTime.now())
                .createdBy(userId)
                .createdByName(userName)
                .summary(summaryMap)
                .itemsData(itemsDataJson)
                .reportFileKey(reportFileKey)
                .reportFileName(reportFileName)
                .reportFileUrl(reportFileUrl)
                .notes(request.getNotes())
                .totalItems(totalItems)
                .matchedItems(matchedItems)
                .missingItems(missingItems)
                .excessItems(excessItems)
                .notInSystemItems(notInSystemItems)
                .build();

        repository.save(record);
        log.info("Created stock reconciliation record: {} ({})", record.getId(), documentNumber);

        return CreateResponse.builder()
                .id(record.getId())
                .documentNumber(documentNumber)
                .reportFileUrl(reportFileUrl)
                .reportFileName(reportFileName)
                .message("Stock reconciliation record created successfully")
                .build();
    }

    /**
     * Get a single record by ID
     */
    public DetailResponse getRecord(String id) {
        StockReconciliationRecord record = repository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + id));
        return mapToDetailResponse(record);
    }

    /**
     * Get a single record by document number
     */
    public DetailResponse getRecordByDocumentNumber(String documentNumber) {
        StockReconciliationRecord record = repository.findByDocumentNumberAndIsDeletedFalse(documentNumber)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + documentNumber));
        return mapToDetailResponse(record);
    }

    /**
     * List records with pagination and filters
     */
    public ListResponse listRecords(String warehouseId, String createdBy,
                                   LocalDateTime startDate, LocalDateTime endDate,
                                   int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<StockReconciliationRecord> recordPage = repository.findWithFilters(
                warehouseId, createdBy, startDate, endDate, pageable);

        List<Response> records = recordPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ListResponse.builder()
                .records(records)
                .page(page)
                .size(size)
                .totalElements(recordPage.getTotalElements())
                .totalPages(recordPage.getTotalPages())
                .build();
    }

    /**
     * Generate presigned download URL for report file
     */
    public DownloadResponse getDownloadUrl(String id) {
        StockReconciliationRecord record = repository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + id));

        if (record.getReportFileKey() == null) {
            throw new IllegalStateException("No report file associated with this record");
        }

        if (r2Service == null) {
            throw new IllegalStateException("R2 storage is not configured");
        }

        // Generate presigned URL with 24-hour expiration
        Duration expiration = Duration.ofHours(24);
        String downloadUrl = r2Service.generateDownloadUrl(record.getReportFileKey(), expiration);

        return DownloadResponse.builder()
                .downloadUrl(downloadUrl)
                .fileName(record.getReportFileName())
                .expiresIn(expiration.getSeconds())
                .build();
    }

    /**
     * Soft delete a record
     */
    @Transactional
    public void deleteRecord(String id) {
        StockReconciliationRecord record = repository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + id));

        record.setIsDeleted(true);
        repository.save(record);
        log.info("Soft deleted stock reconciliation record: {}", id);
    }

    // Helper methods

    private String generateDocumentNumber() {
        return "KK-" + System.currentTimeMillis();
    }

    private String generateFileName(String warehouseName, LocalDateTime date) {
        String safeName = warehouseName != null ?
                warehouseName.replaceAll("[^a-zA-Z0-9\\s-]", "").replaceAll("\\s+", "_") :
                "All_Warehouses";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String dateStr = date != null ? date.format(formatter) :
                LocalDateTime.now().format(formatter);
        return "Bien_ban_kiem_ke_" + safeName + "_" + dateStr + ".docx";
    }

    private String generateFolderPath(LocalDateTime date) {
        if (date == null) date = LocalDateTime.now();
        return String.format("stock-reconciliation/%d/%02d/",
                date.getYear(), date.getMonthValue());
    }

    private Response mapToResponse(StockReconciliationRecord record) {
        Summary summary = null;
        if (record.getSummary() != null) {
            Map<String, Object> summaryMap = record.getSummary();
            summary = Summary.builder()
                    .total(getIntValue(summaryMap, "total"))
                    .match(getIntValue(summaryMap, "match"))
                    .missing(getIntValue(summaryMap, "missing"))
                    .excess(getIntValue(summaryMap, "excess"))
                    .notInMisa(getIntValue(summaryMap, "notInMisa"))
                    .build();
        }

        return Response.builder()
                .id(record.getId())
                .documentNumber(record.getDocumentNumber())
                .warehouseId(record.getWarehouseId())
                .warehouseName(record.getWarehouseName())
                .reconciliationDate(record.getReconciliationDate())
                .createdBy(record.getCreatedBy())
                .createdByName(record.getCreatedByName())
                .summary(summary)
                .totalItems(record.getTotalItems())
                .matchedItems(record.getMatchedItems())
                .missingItems(record.getMissingItems())
                .excessItems(record.getExcessItems())
                .notInSystemItems(record.getNotInSystemItems())
                .reportFileName(record.getReportFileName())
                .reportFileUrl(record.getReportFileUrl())
                .notes(record.getNotes())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private DetailResponse mapToDetailResponse(StockReconciliationRecord record) {
        Response basicResponse = mapToResponse(record);

        List<ReconciliationItem> items = null;
        if (record.getItemsData() != null && !record.getItemsData().isEmpty()) {
            try {
                items = objectMapper.readValue(record.getItemsData(),
                        new TypeReference<List<ReconciliationItem>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse items data JSON: {}", e.getMessage());
            }
        }

        return DetailResponse.builder()
                .id(basicResponse.getId())
                .documentNumber(basicResponse.getDocumentNumber())
                .warehouseId(basicResponse.getWarehouseId())
                .warehouseName(basicResponse.getWarehouseName())
                .reconciliationDate(basicResponse.getReconciliationDate())
                .createdBy(basicResponse.getCreatedBy())
                .createdByName(basicResponse.getCreatedByName())
                .summary(basicResponse.getSummary())
                .items(items)
                .totalItems(basicResponse.getTotalItems())
                .matchedItems(basicResponse.getMatchedItems())
                .missingItems(basicResponse.getMissingItems())
                .excessItems(basicResponse.getExcessItems())
                .notInSystemItems(basicResponse.getNotInSystemItems())
                .reportFileName(basicResponse.getReportFileName())
                .reportFileUrl(basicResponse.getReportFileUrl())
                .notes(basicResponse.getNotes())
                .createdAt(basicResponse.getCreatedAt())
                .updatedAt(basicResponse.getUpdatedAt())
                .build();
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }
}
