package com.mirror.product.controller.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.enums.BusinessType;
import com.mirror.product.enums.PartnerStatus;
import com.mirror.product.enums.PartnerTier;
import com.mirror.product.enums.PartnerType;
import com.mirror.product.service.pod.PodPartnerService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for POD Partner management (Admin).
 */
@RestController
@RequestMapping("/api/v1/admin/pod-partners")
@RequiredArgsConstructor
@Slf4j
public class PodPartnerController {

    private final PodPartnerService partnerService;

    /**
     * Create a new partner
     * POST /api/v1/admin/pod-partners
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PARTNERS_MANAGE')")
    public ResponseEntity<PartnerResponse> createPartner(
            @Valid @RequestBody PartnerCreateRequest request) {
        log.info("REST request to create partner: {}", request.getBusinessName());
        PartnerResponse response = partnerService.createPartner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all partners with optional filtering and pagination
     * GET /api/v1/admin/pod-partners
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PARTNERS_MANAGE')")
    public ResponseEntity<Page<PartnerResponse>> getAllPartners(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PartnerStatus status,
            @RequestParam(required = false) PartnerTier tier,
            @RequestParam(required = false) BusinessType businessType,
            @RequestParam(required = false) PartnerType partnerType,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PartnerSearchCriteria criteria = PartnerSearchCriteria.builder()
                .keyword(keyword)
                .status(status)
                .tier(tier)
                .businessType(businessType)
                .partnerType(partnerType)
                .city(city)
                .country(country)
                .build();

        Page<PartnerResponse> partners = partnerService.searchPartners(criteria, pageable);
        return ResponseEntity.ok(partners);
    }

    /**
     * Get partner by ID
     * GET /api/v1/admin/pod-partners/{partnerId}
     */
    @GetMapping("/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PARTNERS_MANAGE')")
    public ResponseEntity<PartnerResponse> getPartner(
            @PathVariable String partnerId) {
        log.info("REST request to get partner: {}", partnerId);
        PartnerResponse response = partnerService.getPartner(partnerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get partner detail with statistics
     * GET /api/v1/admin/pod-partners/{partnerId}/detail
     */
    @GetMapping("/{partnerId}/detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PARTNERS_MANAGE')")
    public ResponseEntity<PartnerDetailResponse> getPartnerDetail(
            @PathVariable String partnerId) {
        log.info("REST request to get partner detail: {}", partnerId);
        PartnerDetailResponse response = partnerService.getPartnerDetail(partnerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update partner
     * PUT /api/v1/admin/pod-partners/{partnerId}
     */
    @PutMapping("/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PARTNERS_MANAGE')")
    public ResponseEntity<PartnerResponse> updatePartner(
            @PathVariable String partnerId,
            @Valid @RequestBody PartnerUpdateRequest request) {
        log.info("REST request to update partner: {}", partnerId);
        PartnerResponse response = partnerService.updatePartner(partnerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update partner status (approve, activate, suspend, terminate)
     * PATCH /api/v1/admin/pod-partners/{partnerId}/status
     */
    @PatchMapping("/{partnerId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PARTNERS_MANAGE')")
    public ResponseEntity<PartnerResponse> updatePartnerStatus(
            @PathVariable String partnerId,
            @Valid @RequestBody PartnerStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("REST request to update partner status: {} to {}", partnerId, request.getStatus());
        String updatedBy = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        PartnerResponse response = partnerService.updateStatus(partnerId, request, updatedBy);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete partner (soft delete)
     * DELETE /api/v1/admin/pod-partners/{partnerId}
     */
    @DeleteMapping("/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PARTNERS_MANAGE')")
    public ResponseEntity<Void> deletePartner(
            @PathVariable String partnerId) {
        log.info("REST request to delete partner: {}", partnerId);
        partnerService.deletePartner(partnerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get count of active partners
     * GET /api/v1/admin/pod-partners/count
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PARTNERS_MANAGE')")
    public ResponseEntity<Long> getPartnerCount() {
        long count = partnerService.countActive();
        return ResponseEntity.ok(count);
    }
}
