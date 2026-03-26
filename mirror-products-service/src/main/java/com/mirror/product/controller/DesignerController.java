package com.mirror.product.controller;

import com.mirror.product.dto.DesignerRequest;
import com.mirror.product.dto.DesignerResponse;
import com.mirror.product.dto.DesignerDashboardResponse;
import com.mirror.product.dto.DesignProductResponse;
import com.mirror.product.entity.Designer;
import com.mirror.product.service.DesignerService;
import com.mirror.product.service.DesignProductService;
import com.mirror.product.mapper.DesignerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/designers")
@RequiredArgsConstructor
public class DesignerController {

    private final DesignerService designerService;
    private final DesignerMapper designerMapper;
    private final DesignProductService designProductService;

    @GetMapping
    public ResponseEntity<List<DesignerResponse>> getAllDesigners(
            @RequestParam(value = "filter", defaultValue = "all") String filter,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        List<Designer> designers;

        if ("current-user".equals(filter) && userId != null && !userId.trim().isEmpty()) {
            // Filter designers by user ID for designer dashboard
            designers = designerService.findByOwnerUserId(userId);
        } else {
            // Return all designers for admin dashboard or when no filter
            designers = designerService.findAllActive();
        }

        List<DesignerResponse> responses = designers.stream()
                .map(designerMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{designerId}")
    public ResponseEntity<DesignerResponse> getDesignerById(@PathVariable String designerId) {
        return designerService.findActiveById(designerId)
                .map(designerMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<DesignerResponse> getDesignerByCode(@PathVariable String code) {
        return designerService.findByCode(code)
                .map(designerMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/specialty/{specialty}")
    public ResponseEntity<List<DesignerResponse>> getDesignersBySpecialty(@PathVariable String specialty) {
        List<Designer> designers = designerService.findBySpecialty(specialty);
        List<DesignerResponse> responses = designers.stream()
                .map(designerMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/verified")
    public ResponseEntity<List<DesignerResponse>> getVerifiedDesigners() {
        List<Designer> designers = designerService.findVerifiedDesigners();
        List<DesignerResponse> responses = designers.stream()
                .map(designerMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<DesignerResponse>> getFeaturedDesigners() {
        List<Designer> designers = designerService.findFeaturedDesigners();
        List<DesignerResponse> responses = designers.stream()
                .map(designerMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search")
    public ResponseEntity<List<DesignerResponse>> searchDesigners(
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Designer> designers = designerService.searchDesigners(search, pageable);
        List<DesignerResponse> responses = designers.getContent().stream()
                .map(designerMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(designers.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(designers.getTotalPages()))
                .body(responses);
    }

    @PostMapping
    public ResponseEntity<?> createDesigner(
            @Valid @RequestBody DesignerRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Designer designer = designerMapper.toEntity(request);

            // Set owner user ID from JWT token
            if (userId != null && !userId.trim().isEmpty()) {
                designer.setOwnerUserId(userId);
            }

            Designer savedDesigner = designerService.save(designer);
            DesignerResponse response = designerMapper.toResponse(savedDesigner);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while creating designer");
        }
    }

    @PutMapping("/{designerId}")
    public ResponseEntity<?> updateDesigner(@PathVariable String designerId, @RequestBody DesignerRequest request) {
        try {
            Designer existingDesigner = designerService.findActiveById(designerId)
                    .orElseThrow(() -> new RuntimeException("Designer not found"));
            designerMapper.updateEntity(existingDesigner, request);
            Designer updatedDesigner = designerService.save(existingDesigner);
            DesignerResponse response = designerMapper.toResponse(updatedDesigner);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating designer");
        }
    }

    @DeleteMapping("/{designerId}")
    public ResponseEntity<?> deleteDesigner(@PathVariable String designerId) {
        try {
            designerService.softDeleteById(designerId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting designer");
        }
    }

    @PatchMapping("/{designerId}/deactivate")
    public ResponseEntity<?> deactivateDesigner(@PathVariable String designerId) {
        try {
            designerService.deactivateById(designerId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deactivating designer");
        }
    }

    @GetMapping("/exists/{code}")
    public ResponseEntity<Boolean> checkDesignerExists(@PathVariable String code) {
        boolean exists = designerService.existsByCode(code);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        long count = designerService.countActive();
        return ResponseEntity.ok(count);
    }

    /**
     * Get design products for a specific designer
     * GET /api/designers/{designerId}/designs
     */
    @GetMapping("/{designerId}/designs")
    public ResponseEntity<List<DesignProductResponse>> getDesignerDesigns(@PathVariable String designerId) {
        try {
            List<DesignProductResponse> designs = designProductService.getDesignsByDesignerId(designerId);
            return ResponseEntity.ok(designs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get dashboard data for a designer by designer ID
     * GET /api/designers/{designerId}/dashboard
     */
    @GetMapping("/{designerId}/dashboard")
    public ResponseEntity<DesignerDashboardResponse> getDesignerDashboard(@PathVariable String designerId) {
        try {
            DesignerDashboardResponse dashboard = designerService.getDashboardData(designerId);
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Get dashboard data for current user (designer)
     * GET /api/designers/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DesignerDashboardResponse> getCurrentUserDashboard(
            @RequestHeader(value = "X-User-Id", required = true) String userId) {
        try {
            DesignerDashboardResponse dashboard = designerService.getDashboardDataByUserId(userId);
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}