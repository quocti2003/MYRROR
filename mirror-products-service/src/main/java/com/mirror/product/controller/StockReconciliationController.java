package com.mirror.product.controller;

import com.mirror.product.dto.StockReconciliationDTO;
import com.mirror.product.dto.StockReconciliationDTO.*;
import com.mirror.product.service.StockReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-reconciliation")
@RequiredArgsConstructor
@Slf4j
public class StockReconciliationController {

    private final StockReconciliationService reconciliationService;

    /**
     * Create a new stock reconciliation record with report file
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createRecord(
            @RequestPart("data") CreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        try {
            log.info("Creating stock reconciliation record for warehouse: {}", request.getWarehouseName());

            byte[] fileContent = null;
            if (file != null && !file.isEmpty()) {
                fileContent = file.getBytes();
            }

            CreateResponse response = reconciliationService.createRecord(
                    request, fileContent, userId, userName);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating reconciliation record: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating reconciliation record: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create reconciliation record: " + e.getMessage()));
        }
    }

    /**
     * Get a single record by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRecord(
            @PathVariable String id,
            @RequestParam(value = "includeItems", defaultValue = "false") boolean includeItems) {
        try {
            DetailResponse response = reconciliationService.getRecord(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting reconciliation record: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get reconciliation record"));
        }
    }

    /**
     * List reconciliation records with pagination and filters
     */
    @GetMapping
    public ResponseEntity<?> listRecords(
            @RequestParam(value = "warehouseId", required = false) String warehouseId,
            @RequestParam(value = "createdBy", required = false) String createdBy,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        try {
            ListResponse response = reconciliationService.listRecords(
                    warehouseId, createdBy, startDate, endDate, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error listing reconciliation records: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to list reconciliation records"));
        }
    }

    /**
     * Get download URL for report file
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> getDownloadUrl(@PathVariable String id) {
        try {
            DownloadResponse response = reconciliationService.getDownloadUrl(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting download URL: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get download URL"));
        }
    }

    /**
     * Delete a reconciliation record (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecord(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            reconciliationService.deleteRecord(id);
            return ResponseEntity.ok(Map.of("message", "Record deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting reconciliation record: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete reconciliation record"));
        }
    }
}
