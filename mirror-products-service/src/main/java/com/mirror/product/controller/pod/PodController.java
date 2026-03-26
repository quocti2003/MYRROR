package com.mirror.product.controller.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.enums.PodStatus;
import com.mirror.product.service.pod.PodService;
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

import java.util.List;

/**
 * REST Controller for POD management (Admin).
 * SPRINT_2: Uncomment @RestController when ready to test Sprint 2
 */
@RestController  // ENABLED_SPRINT_2
@RequestMapping("/api/v1/admin/pods")
@RequiredArgsConstructor
@Slf4j
public class PodController {

    private final PodService podService;

    /**
     * Create a new POD
     * POST /api/v1/admin/pods
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PODS_MANAGE')")
    public ResponseEntity<PodResponse> createPod(
            @Valid @RequestBody PodCreateRequest request) {
        log.info("REST request to create POD: {}", request.getName());
        PodResponse response = podService.createPod(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all PODs with optional filtering and pagination
     * GET /api/v1/admin/pods
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PODS_MANAGE')")
    public ResponseEntity<Page<PodResponse>> getAllPods(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) PodStatus status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PodSearchCriteria criteria = PodSearchCriteria.builder()
                .keyword(keyword)
                .partnerId(partnerId)
                .status(status)
                .city(city)
                .country(country)
                .productId(productId)
                .build();

        Page<PodResponse> pods = podService.searchPods(criteria, pageable);
        return ResponseEntity.ok(pods);
    }

    /**
     * Get POD by ID
     * GET /api/v1/admin/pods/{podId}
     */
    @GetMapping("/{podId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PODS_MANAGE')")
    public ResponseEntity<PodResponse> getPod(
            @PathVariable String podId) {
        log.info("REST request to get POD: {}", podId);
        PodResponse response = podService.getPod(podId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get POD detail with statistics
     * GET /api/v1/admin/pods/{podId}/detail
     */
    @GetMapping("/{podId}/detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PODS_MANAGE')")
    public ResponseEntity<PodDetailResponse> getPodDetail(
            @PathVariable String podId) {
        log.info("REST request to get POD detail: {}", podId);
        PodDetailResponse response = podService.getPodDetail(podId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update POD
     * PUT /api/v1/admin/pods/{podId}
     */
    @PutMapping("/{podId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PODS_MANAGE')")
    public ResponseEntity<PodResponse> updatePod(
            @PathVariable String podId,
            @Valid @RequestBody PodUpdateRequest request) {
        log.info("REST request to update POD: {}", podId);
        PodResponse response = podService.updatePod(podId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update POD status
     * PATCH /api/v1/admin/pods/{podId}/status
     */
    @PatchMapping("/{podId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PODS_MANAGE')")
    public ResponseEntity<PodResponse> updatePodStatus(
            @PathVariable String podId,
            @Valid @RequestBody PodStatusUpdateRequest request) {
        log.info("REST request to update POD status: {} to {}", podId, request.getStatus());
        PodResponse response = podService.updateStatus(podId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Assign products to POD
     * PUT /api/v1/admin/pods/{podId}/products
     */
    @PutMapping("/{podId}/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PODS_MANAGE')")
    public ResponseEntity<PodResponse> assignProducts(
            @PathVariable String podId,
            @Valid @RequestBody PodProductAssignRequest request) {
        log.info("REST request to assign products to POD: {}", podId);
        PodResponse response = podService.assignProducts(podId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a product from POD
     * DELETE /api/v1/admin/pods/{podId}/products/{productId}
     */
    @DeleteMapping("/{podId}/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PODS_MANAGE')")
    public ResponseEntity<PodResponse> removeProduct(
            @PathVariable String podId,
            @PathVariable String productId) {
        log.info("REST request to remove product {} from POD: {}", productId, podId);
        PodResponse response = podService.removeProduct(podId, productId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete POD (soft delete)
     * DELETE /api/v1/admin/pods/{podId}
     */
    @DeleteMapping("/{podId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PODS_MANAGE')")
    public ResponseEntity<Void> deletePod(
            @PathVariable String podId) {
        log.info("REST request to delete POD: {}", podId);
        podService.deletePod(podId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get count of active PODs
     * GET /api/v1/admin/pods/count
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PODS_MANAGE')")
    public ResponseEntity<Long> getPodCount() {
        long count = podService.countActive();
        return ResponseEntity.ok(count);
    }

    /**
     * Get PODs by partner
     * GET /api/v1/admin/pods/partner/{partnerId}
     */
    @GetMapping("/partner/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PODS_MANAGE')")
    public ResponseEntity<Page<PodResponse>> getPodsByPartner(
            @PathVariable String partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PodResponse> pods = podService.getPodsByPartner(partnerId, pageable);
        return ResponseEntity.ok(pods);
    }

    /**
     * Get distinct cities for filtering
     * GET /api/v1/admin/pods/cities
     */
    @GetMapping("/cities")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_PODS_MANAGE')")
    public ResponseEntity<List<String>> getDistinctCities() {
        List<String> cities = podService.getDistinctCities();
        return ResponseEntity.ok(cities);
    }
}
