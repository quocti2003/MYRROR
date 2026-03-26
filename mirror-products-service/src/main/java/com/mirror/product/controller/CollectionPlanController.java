package com.mirror.product.controller;

import com.mirror.product.dto.collectionplan.*;
import com.mirror.product.entity.CollectionPlan;
import com.mirror.product.mapper.CollectionPlanMapper;
import com.mirror.product.service.CollectionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/collection-plans")
@RequiredArgsConstructor
@Slf4j
public class CollectionPlanController {

    private final CollectionPlanService service;
    private final CollectionPlanMapper mapper;

    /**
     * List collection plans with pagination, search, and status filter
     */
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<CollectionPlan> planPage = service.findAll(search, status, pageable);

            List<CollectionPlanResponse> responses = new ArrayList<>();
            for (CollectionPlan plan : planPage.getContent()) {
                CollectionPlanResponse response = mapper.toListResponse(plan);
                response.setHasProductionPlans(service.hasProductionPlans(plan.getId()));
                responses.add(response);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("content", responses);
            result.put("currentPage", planPage.getNumber());
            result.put("totalItems", planPage.getTotalElements());
            result.put("totalPages", planPage.getTotalPages());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching collection plans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching collection plans");
        }
    }

    /**
     * Get collection plan by ID with items
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        try {
            Optional<CollectionPlan> planOpt = service.findByIdWithItems(id);
            if (planOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            CollectionPlan plan = planOpt.get();
            CollectionPlanResponse response = mapper.toResponse(plan);
            response.setHasProductionPlans(service.hasProductionPlans(id));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching collection plan: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching collection plan");
        }
    }

    /**
     * Create a new collection plan with optional items
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CollectionPlanCreateRequest request) {
        try {
            CollectionPlan plan = service.create(request);
            // Re-fetch with items to get full response
            CollectionPlan saved = service.findByIdWithItems(plan.getId()).orElse(plan);
            CollectionPlanResponse response = mapper.toResponse(saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid request for creating collection plan: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating collection plan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating collection plan");
        }
    }

    /**
     * Update a collection plan
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable UUID id,
            @Valid @RequestBody CollectionPlanUpdateRequest request) {
        try {
            CollectionPlan plan = service.update(id, request);
            CollectionPlan saved = service.findByIdWithItems(plan.getId()).orElse(plan);
            CollectionPlanResponse response = mapper.toResponse(saved);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error updating collection plan {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating collection plan: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating collection plan");
        }
    }

    /**
     * Soft delete (cancel) a collection plan
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        try {
            service.softDelete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot delete collection plan {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting collection plan: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while deleting collection plan");
        }
    }

    /**
     * Update status with transition validation
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CollectionPlanStatusUpdateRequest request) {
        try {
            CollectionPlan plan = service.updateStatus(id, request.getStatus());
            CollectionPlanResponse response = mapper.toListResponse(plan);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error updating collection plan status {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating collection plan status: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating collection plan status");
        }
    }

    /**
     * Get status counts
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Map<String, Long> stats = service.getStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching collection plan stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching stats");
        }
    }

    // ==================== Item Endpoints ====================

    /**
     * Add item to collection plan
     */
    @PostMapping("/{id}/items")
    public ResponseEntity<?> addItem(
            @PathVariable UUID id,
            @Valid @RequestBody CollectionPlanItemRequest request) {
        try {
            CollectionPlan plan = service.addItem(id, request);
            CollectionPlan saved = service.findByIdWithItems(plan.getId()).orElse(plan);
            CollectionPlanResponse response = mapper.toResponse(saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error adding item to collection plan {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error adding item to collection plan: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while adding item");
        }
    }

    /**
     * Update item in collection plan
     */
    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<?> updateItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody CollectionPlanItemRequest request) {
        try {
            CollectionPlan plan = service.updateItem(id, itemId, request);
            CollectionPlan saved = service.findByIdWithItems(plan.getId()).orElse(plan);
            CollectionPlanResponse response = mapper.toResponse(saved);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error updating item in collection plan {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating item in collection plan: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating item");
        }
    }

    /**
     * Remove item from collection plan
     */
    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<?> removeItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        try {
            CollectionPlan plan = service.removeItem(id, itemId);
            CollectionPlan saved = service.findByIdWithItems(plan.getId()).orElse(plan);
            CollectionPlanResponse response = mapper.toResponse(saved);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error removing item from collection plan {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error removing item from collection plan: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while removing item");
        }
    }
}
