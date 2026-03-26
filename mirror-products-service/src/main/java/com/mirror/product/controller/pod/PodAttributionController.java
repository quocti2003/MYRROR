package com.mirror.product.controller.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.enums.AttributionStatus;
import com.mirror.product.enums.AttributionType;
import com.mirror.product.service.pod.PodAttributionService;
import jakarta.servlet.http.HttpServletRequest;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Attribution management (Admin).
 * SPRINT_4: Uncomment @RestController when ready to test Sprint 4
 */
@RestController  // ENABLED_SPRINT_4
@RequestMapping("/api/v1/admin/pod-attributions")
@RequiredArgsConstructor
@Slf4j
public class PodAttributionController {

    private final PodAttributionService attributionService;

    /**
     * Process attribution for an order (auto-detect from cookies/session).
     * POST /api/v1/admin/pod-attributions/process/{orderId}
     */
    @PostMapping("/process/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<AttributionResponse> processOrderAttribution(
            @PathVariable String orderId,
            HttpServletRequest request) {
        log.info("REST request to process attribution for order: {}", orderId);
        Optional<AttributionResponse> response = attributionService.processOrderAttribution(orderId, request);
        return response.map(r -> ResponseEntity.status(HttpStatus.CREATED).body(r))
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Process attribution for an order using session ID.
     * POST /api/v1/admin/pod-attributions/process/{orderId}/session
     */
    @PostMapping("/process/{orderId}/session")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<AttributionResponse> processOrderAttributionBySession(
            @PathVariable String orderId,
            @RequestParam String sessionId) {
        log.info("REST request to process attribution for order: {} with session: {}", orderId, sessionId);
        Optional<AttributionResponse> response = attributionService.processOrderAttributionBySession(orderId, sessionId);
        return response.map(r -> ResponseEntity.status(HttpStatus.CREATED).body(r))
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Manually create an attribution.
     * POST /api/v1/admin/pod-attributions
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<AttributionResponse> createAttribution(
            @Valid @RequestBody AttributionCreateRequest request) {
        log.info("REST request to create attribution for order: {} to POD: {}",
                request.getOrderId(), request.getPodId());
        AttributionResponse response = attributionService.createAttribution(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all attributions with optional filtering and pagination.
     * GET /api/v1/admin/pod-attributions
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<Page<AttributionResponse>> getAllAttributions(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String podId,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) String qrCodeId,
            @RequestParam(required = false) AttributionType attributionType,
            @RequestParam(required = false) AttributionStatus status,
            @RequestParam(required = false) Instant orderPlacedAfter,
            @RequestParam(required = false) Instant orderPlacedBefore,
            @RequestParam(required = false) BigDecimal minOrderAmount,
            @RequestParam(required = false) BigDecimal maxOrderAmount,
            @RequestParam(required = false) Integer minDaysToConversion,
            @RequestParam(required = false) Integer maxDaysToConversion,
            @RequestParam(required = false) Boolean hasCommission,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderPlacedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        AttributionSearchCriteria criteria = AttributionSearchCriteria.builder()
                .orderId(orderId)
                .podId(podId)
                .partnerId(partnerId)
                .qrCodeId(qrCodeId)
                .attributionType(attributionType)
                .status(status)
                .orderPlacedAfter(orderPlacedAfter)
                .orderPlacedBefore(orderPlacedBefore)
                .minOrderAmount(minOrderAmount)
                .maxOrderAmount(maxOrderAmount)
                .minDaysToConversion(minDaysToConversion)
                .maxDaysToConversion(maxDaysToConversion)
                .hasCommission(hasCommission)
                .build();

        Page<AttributionResponse> attributions = attributionService.searchAttributions(criteria, pageable);
        return ResponseEntity.ok(attributions);
    }

    /**
     * Get attribution by ID.
     * GET /api/v1/admin/pod-attributions/{attributionId}
     */
    @GetMapping("/{attributionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<AttributionResponse> getAttribution(
            @PathVariable String attributionId) {
        log.info("REST request to get attribution: {}", attributionId);
        AttributionResponse response = attributionService.getAttribution(attributionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get attributions by order ID.
     * GET /api/v1/admin/pod-attributions/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<List<AttributionResponse>> getAttributionsByOrder(
            @PathVariable String orderId) {
        log.info("REST request to get attributions for order: {}", orderId);
        List<AttributionResponse> responses = attributionService.getAttributionsByOrder(orderId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get attributions by partner ID.
     * GET /api/v1/admin/pod-attributions/partner/{partnerId}
     */
    @GetMapping("/partner/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<Page<AttributionResponse>> getAttributionsByPartner(
            @PathVariable String partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderPlacedAt").descending());
        Page<AttributionResponse> attributions = attributionService.getAttributionsByPartner(partnerId, pageable);
        return ResponseEntity.ok(attributions);
    }

    /**
     * Update attribution status.
     * PATCH /api/v1/admin/pod-attributions/{attributionId}/status
     */
    @PatchMapping("/{attributionId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<AttributionResponse> updateAttributionStatus(
            @PathVariable String attributionId,
            @Valid @RequestBody AttributionStatusUpdateRequest request) {
        log.info("REST request to update attribution status: {} to {}", attributionId, request.getStatus());
        AttributionResponse response = attributionService.updateStatus(attributionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirm attribution.
     * POST /api/v1/admin/pod-attributions/{attributionId}/confirm
     */
    @PostMapping("/{attributionId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<AttributionResponse> confirmAttribution(
            @PathVariable String attributionId) {
        log.info("REST request to confirm attribution: {}", attributionId);
        AttributionResponse response = attributionService.confirmAttribution(attributionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel attribution.
     * POST /api/v1/admin/pod-attributions/{attributionId}/cancel
     */
    @PostMapping("/{attributionId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<AttributionResponse> cancelAttribution(
            @PathVariable String attributionId,
            @RequestParam(required = false) String reason) {
        log.info("REST request to cancel attribution: {}", attributionId);
        AttributionResponse response = attributionService.cancelAttribution(attributionId, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete attribution (soft delete).
     * DELETE /api/v1/admin/pod-attributions/{attributionId}
     */
    @DeleteMapping("/{attributionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<Void> deleteAttribution(
            @PathVariable String attributionId) {
        log.info("REST request to delete attribution: {}", attributionId);
        attributionService.deleteAttribution(attributionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get attribution statistics for a partner.
     * GET /api/v1/admin/pod-attributions/stats/partner/{partnerId}
     */
    @GetMapping("/stats/partner/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<AttributionStatistics> getPartnerStatistics(
            @PathVariable String partnerId,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        log.info("REST request to get attribution statistics for partner: {} from {} to {}", partnerId, start, end);
        AttributionStatistics stats = attributionService.getPartnerStatistics(partnerId, start, end);
        return ResponseEntity.ok(stats);
    }
}
