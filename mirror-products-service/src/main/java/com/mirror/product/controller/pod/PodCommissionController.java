package com.mirror.product.controller.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.enums.CommissionStatus;
import com.mirror.product.service.pod.PodCommissionService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Commission management (Admin).
 * SPRINT_5: Uncomment @RestController when ready to test Sprint 5
 */
@RestController  // ENABLED_SPRINT_5
@RequestMapping("/api/v1/admin/pod-commissions")
@RequiredArgsConstructor
@Slf4j
public class PodCommissionController {

    private final PodCommissionService commissionService;

    /**
     * Generate commission for a single partner.
     * POST /api/v1/admin/pod-commissions/generate
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_MANAGE')")
    public ResponseEntity<CommissionResponse> generateCommission(
            @Valid @RequestBody CommissionGenerateRequest request) {
        log.info("REST request to generate commission for partner: {} for period {} to {}",
                request.getPartnerId(), request.getPeriodStart(), request.getPeriodEnd());
        CommissionResponse response = commissionService.generateCommission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Generate commissions for multiple partners in batch.
     * POST /api/v1/admin/pod-commissions/generate/batch
     */
    @PostMapping("/generate/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_MANAGE')")
    public ResponseEntity<List<CommissionResponse>> generateCommissionsBatch(
            @Valid @RequestBody CommissionBatchGenerateRequest request) {
        log.info("REST request to batch generate commissions for period {} to {}",
                request.getPeriodStart(), request.getPeriodEnd());
        List<CommissionResponse> responses = commissionService.generateCommissionsBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Generate monthly commissions for last month.
     * POST /api/v1/admin/pod-commissions/generate/monthly
     */
    @PostMapping("/generate/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_MANAGE')")
    public ResponseEntity<List<CommissionResponse>> generateMonthlyCommissions() {
        log.info("REST request to generate monthly commissions");
        List<CommissionResponse> responses = commissionService.generateMonthlyCommissions();
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Get all commissions with optional filtering and pagination.
     * GET /api/v1/admin/pod-commissions
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_MANAGE')")
    public ResponseEntity<Page<CommissionResponse>> getAllCommissions(
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) CommissionStatus status,
            @RequestParam(required = false) LocalDate periodStartAfter,
            @RequestParam(required = false) LocalDate periodStartBefore,
            @RequestParam(required = false) LocalDate periodEndAfter,
            @RequestParam(required = false) LocalDate periodEndBefore,
            @RequestParam(required = false) BigDecimal minFinalAmount,
            @RequestParam(required = false) BigDecimal maxFinalAmount,
            @RequestParam(required = false) Boolean isPaid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "periodStart") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        CommissionSearchCriteria criteria = CommissionSearchCriteria.builder()
                .partnerId(partnerId)
                .status(status)
                .periodStartAfter(periodStartAfter)
                .periodStartBefore(periodStartBefore)
                .periodEndAfter(periodEndAfter)
                .periodEndBefore(periodEndBefore)
                .minFinalAmount(minFinalAmount)
                .maxFinalAmount(maxFinalAmount)
                .isPaid(isPaid)
                .build();

        Page<CommissionResponse> commissions = commissionService.searchCommissions(criteria, pageable);
        return ResponseEntity.ok(commissions);
    }

    /**
     * Get commission by ID.
     * GET /api/v1/admin/pod-commissions/{commissionId}
     */
    @GetMapping("/{commissionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_MANAGE')")
    public ResponseEntity<CommissionResponse> getCommission(
            @PathVariable String commissionId) {
        log.info("REST request to get commission: {}", commissionId);
        CommissionResponse response = commissionService.getCommission(commissionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get commission detail with attributions.
     * GET /api/v1/admin/pod-commissions/{commissionId}/detail
     */
    @GetMapping("/{commissionId}/detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_MANAGE')")
    public ResponseEntity<CommissionDetailResponse> getCommissionDetail(
            @PathVariable String commissionId) {
        log.info("REST request to get commission detail: {}", commissionId);
        CommissionDetailResponse response = commissionService.getCommissionDetail(commissionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get commissions by partner.
     * GET /api/v1/admin/pod-commissions/partner/{partnerId}
     */
    @GetMapping("/partner/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_MANAGE')")
    public ResponseEntity<Page<CommissionResponse>> getCommissionsByPartner(
            @PathVariable String partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("periodStart").descending());
        Page<CommissionResponse> commissions = commissionService.getCommissionsByPartner(partnerId, pageable);
        return ResponseEntity.ok(commissions);
    }

    /**
     * Add adjustment to a commission.
     * POST /api/v1/admin/pod-commissions/{commissionId}/adjust
     */
    @PostMapping("/{commissionId}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_MANAGE')")
    public ResponseEntity<CommissionResponse> addAdjustment(
            @PathVariable String commissionId,
            @Valid @RequestBody CommissionAdjustRequest request) {
        log.info("REST request to add adjustment to commission: {}", commissionId);
        CommissionResponse response = commissionService.addAdjustment(commissionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve a commission.
     * POST /api/v1/admin/pod-commissions/{commissionId}/approve
     */
    @PostMapping("/{commissionId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_APPROVE')")
    public ResponseEntity<CommissionResponse> approveCommission(
            @PathVariable String commissionId,
            Authentication authentication) {
        String approver = authentication != null ? authentication.getName() : "system";
        log.info("REST request to approve commission: {} by {}", commissionId, approver);
        CommissionResponse response = commissionService.approveCommission(commissionId, approver);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark commission as paid.
     * POST /api/v1/admin/pod-commissions/{commissionId}/pay
     */
    @PostMapping("/{commissionId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_PAY')")
    public ResponseEntity<CommissionResponse> markAsPaid(
            @PathVariable String commissionId,
            @Valid @RequestBody CommissionPaymentRequest request) {
        log.info("REST request to mark commission as paid: {}", commissionId);
        CommissionResponse response = commissionService.markAsPaid(commissionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a commission.
     * POST /api/v1/admin/pod-commissions/{commissionId}/cancel
     */
    @PostMapping("/{commissionId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_MANAGE')")
    public ResponseEntity<CommissionResponse> cancelCommission(
            @PathVariable String commissionId,
            @RequestParam(required = false) String reason) {
        log.info("REST request to cancel commission: {}", commissionId);
        CommissionResponse response = commissionService.cancelCommission(commissionId, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete commission (soft delete).
     * DELETE /api/v1/admin/pod-commissions/{commissionId}
     */
    @DeleteMapping("/{commissionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_MANAGE')")
    public ResponseEntity<Void> deleteCommission(
            @PathVariable String commissionId) {
        log.info("REST request to delete commission: {}", commissionId);
        commissionService.deleteCommission(commissionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get overall commission summary.
     * GET /api/v1/admin/pod-commissions/summary
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_MANAGE')")
    public ResponseEntity<CommissionSummary> getCommissionSummary() {
        log.info("REST request to get commission summary");
        CommissionSummary summary = commissionService.getCommissionSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Get commission summary for a specific partner.
     * GET /api/v1/admin/pod-commissions/summary/partner/{partnerId}
     */
    @GetMapping("/summary/partner/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_COMMISSIONS_MANAGE')")
    public ResponseEntity<CommissionSummary> getPartnerCommissionSummary(
            @PathVariable String partnerId) {
        log.info("REST request to get commission summary for partner: {}", partnerId);
        CommissionSummary summary = commissionService.getPartnerCommissionSummary(partnerId);
        return ResponseEntity.ok(summary);
    }
}
