package com.mirror.product.controller.pod;

import com.mirror.product.dto.pod.PodUserAttributionResponse;
import com.mirror.product.enums.PodUserAttributionStatus;
import com.mirror.product.service.pod.PodUserAttributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/pod-user-attributions")
@RequiredArgsConstructor
@Slf4j
public class PodUserAttributionController {

    private final PodUserAttributionService service;

    /**
     * Get all user-POD attributions with optional filters.
     * GET /api/v1/admin/pod-user-attributions
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<Page<PodUserAttributionResponse>> getAllAttributions(
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) String podId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) PodUserAttributionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("REST request to get user attributions - partnerId: {}, podId: {}, userId: {}, status: {}",
                partnerId, podId, userId, status);

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PodUserAttributionResponse> response = service.getAllAttributions(partnerId, podId, userId, status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user attribution by ID.
     * GET /api/v1/admin/pod-user-attributions/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<PodUserAttributionResponse> getById(@PathVariable String id) {
        log.info("REST request to get user attribution: {}", id);
        PodUserAttributionResponse response = service.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user attributions by partner.
     * GET /api/v1/admin/pod-user-attributions/partner/{partnerId}
     */
    @GetMapping("/partner/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_ATTRIBUTIONS_MANAGE')")
    public ResponseEntity<Page<PodUserAttributionResponse>> getByPartner(
            @PathVariable String partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("REST request to get user attributions for partner: {}", partnerId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PodUserAttributionResponse> response = service.getByPartner(partnerId, pageable);
        return ResponseEntity.ok(response);
    }
}
