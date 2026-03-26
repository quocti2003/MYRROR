package com.mirror.product.controller.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.enums.QrCodeStatus;
import com.mirror.product.service.pod.PodQrCodeService;
import com.mirror.product.service.pod.PodQrScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST Controller for QR Code management (Admin).
 * SPRINT_3: Uncomment @RestController when ready to test Sprint 3
 */
@RestController  // ENABLED_SPRINT_3
@RequestMapping("/api/v1/admin/pod-qrcodes")
@RequiredArgsConstructor
@Slf4j
public class PodQrCodeController {

    private final PodQrCodeService qrCodeService;
    private final PodQrScanService scanService;

    /**
     * Create a single QR code
     * POST /api/v1/admin/pod-qrcodes
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<QrCodeResponse> createQrCode(
            @Valid @RequestBody QrCodeCreateRequest request) {
        log.info("REST request to create QR code for POD: {} and product: {}",
                request.getPodId(), request.getProductId());
        QrCodeResponse response = qrCodeService.createQrCode(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create QR codes in batch
     * POST /api/v1/admin/pod-qrcodes/batch
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<List<QrCodeResponse>> createQrCodesBatch(
            @Valid @RequestBody QrCodeBatchCreateRequest request) {
        log.info("REST request to create batch QR codes for POD: {} with {} products",
                request.getPodId(), request.getProductIds().size());
        List<QrCodeResponse> response = qrCodeService.createQrCodesBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all QR codes with optional filtering and pagination
     * GET /api/v1/admin/pod-qrcodes
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<Page<QrCodeResponse>> getAllQrCodes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String podId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) QrCodeStatus status,
            @RequestParam(required = false) Instant createdAfter,
            @RequestParam(required = false) Instant createdBefore,
            @RequestParam(required = false) Long minScanCount,
            @RequestParam(required = false) Long maxScanCount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        QrCodeSearchCriteria criteria = QrCodeSearchCriteria.builder()
                .keyword(keyword)
                .podId(podId)
                .productId(productId)
                .partnerId(partnerId)
                .status(status)
                .createdAfter(createdAfter)
                .createdBefore(createdBefore)
                .minScanCount(minScanCount)
                .maxScanCount(maxScanCount)
                .build();

        Page<QrCodeResponse> qrCodes = qrCodeService.searchQrCodes(criteria, pageable);
        return ResponseEntity.ok(qrCodes);
    }

    /**
     * Get QR code by ID
     * GET /api/v1/admin/pod-qrcodes/{qrCodeId}
     */
    @GetMapping("/{qrCodeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<QrCodeResponse> getQrCode(
            @PathVariable String qrCodeId) {
        log.info("REST request to get QR code: {}", qrCodeId);
        QrCodeResponse response = qrCodeService.getQrCode(qrCodeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get QR code by short code
     * GET /api/v1/admin/pod-qrcodes/code/{shortCode}
     */
    @GetMapping("/code/{shortCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<QrCodeResponse> getQrCodeByShortCode(
            @PathVariable String shortCode) {
        log.info("REST request to get QR code by short code: {}", shortCode);
        QrCodeResponse response = qrCodeService.getQrCodeByShortCode(shortCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Get QR code detail with statistics
     * GET /api/v1/admin/pod-qrcodes/{qrCodeId}/detail
     */
    @GetMapping("/{qrCodeId}/detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<QrCodeDetailResponse> getQrCodeDetail(
            @PathVariable String qrCodeId) {
        log.info("REST request to get QR code detail: {}", qrCodeId);
        QrCodeDetailResponse response = qrCodeService.getQrCodeDetail(qrCodeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all QR codes for a POD
     * GET /api/v1/admin/pod-qrcodes/pod/{podId}
     */
    @GetMapping("/pod/{podId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<Page<QrCodeResponse>> getQrCodesByPod(
            @PathVariable String podId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<QrCodeResponse> qrCodes = qrCodeService.getQrCodesByPod(podId, pageable);
        return ResponseEntity.ok(qrCodes);
    }

    /**
     * Deactivate a QR code
     * PATCH /api/v1/admin/pod-qrcodes/{qrCodeId}/deactivate
     */
    @PatchMapping("/{qrCodeId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<QrCodeResponse> deactivateQrCode(
            @PathVariable String qrCodeId) {
        log.info("REST request to deactivate QR code: {}", qrCodeId);
        QrCodeResponse response = qrCodeService.deactivateQrCode(qrCodeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reactivate a QR code
     * PATCH /api/v1/admin/pod-qrcodes/{qrCodeId}/reactivate
     */
    @PatchMapping("/{qrCodeId}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<QrCodeResponse> reactivateQrCode(
            @PathVariable String qrCodeId) {
        log.info("REST request to reactivate QR code: {}", qrCodeId);
        QrCodeResponse response = qrCodeService.reactivateQrCode(qrCodeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Regenerate QR code image
     * POST /api/v1/admin/pod-qrcodes/{qrCodeId}/regenerate-image
     */
    @PostMapping("/{qrCodeId}/regenerate-image")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<QrCodeResponse> regenerateQrImage(
            @PathVariable String qrCodeId) {
        log.info("REST request to regenerate QR image for code: {}", qrCodeId);
        QrCodeResponse response = qrCodeService.regenerateQrImage(qrCodeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete QR code (soft delete)
     * DELETE /api/v1/admin/pod-qrcodes/{qrCodeId}
     */
    @DeleteMapping("/{qrCodeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<Void> deleteQrCode(
            @PathVariable String qrCodeId) {
        log.info("REST request to delete QR code: {}", qrCodeId);
        qrCodeService.deleteQrCode(qrCodeId);
        return ResponseEntity.noContent().build();
    }

    // ========== Scan History Endpoints ==========

    /**
     * Get scan history for a QR code
     * GET /api/v1/admin/pod-qrcodes/{qrCodeId}/scans
     */
    @GetMapping("/{qrCodeId}/scans")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<Page<ScanRecordResponse>> getQrCodeScans(
            @PathVariable String qrCodeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("scannedAt").descending());
        Page<ScanRecordResponse> scans = scanService.getScansByQrCode(qrCodeId, pageable);
        return ResponseEntity.ok(scans);
    }

    /**
     * Get scan by ID
     * GET /api/v1/admin/pod-qrcodes/scans/{scanId}
     */
    @GetMapping("/scans/{scanId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<ScanRecordResponse> getScan(
            @PathVariable String scanId) {
        log.info("REST request to get scan: {}", scanId);
        ScanRecordResponse response = scanService.getScan(scanId);
        return ResponseEntity.ok(response);
    }

    /**
     * Search scans with criteria
     * GET /api/v1/admin/pod-qrcodes/scans
     */
    @GetMapping("/scans")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<Page<ScanRecordResponse>> searchScans(
            @RequestParam(required = false) String qrCodeId,
            @RequestParam(required = false) String podId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) Instant scannedAfter,
            @RequestParam(required = false) Instant scannedBefore,
            @RequestParam(required = false) Boolean uniqueOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "scannedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        ScanSearchCriteria criteria = ScanSearchCriteria.builder()
                .qrCodeId(qrCodeId)
                .podId(podId)
                .productId(productId)
                .partnerId(partnerId)
                .scannedAfter(scannedAfter)
                .scannedBefore(scannedBefore)
                .uniqueOnly(uniqueOnly)
                .build();

        Page<ScanRecordResponse> scans = scanService.searchScans(criteria, pageable);
        return ResponseEntity.ok(scans);
    }

    /**
     * Get scan statistics for a partner
     * GET /api/v1/admin/pod-qrcodes/stats/partner/{partnerId}
     */
    @GetMapping("/stats/partner/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_QRCODES_MANAGE')")
    public ResponseEntity<PodQrScanService.ScanStatistics> getPartnerScanStatistics(
            @PathVariable String partnerId,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        log.info("REST request to get scan statistics for partner: {} from {} to {}", partnerId, start, end);
        PodQrScanService.ScanStatistics stats = scanService.getPartnerScanStatistics(partnerId, start, end);
        return ResponseEntity.ok(stats);
    }
}
