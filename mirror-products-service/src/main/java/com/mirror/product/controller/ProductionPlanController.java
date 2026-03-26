package com.mirror.product.controller;

import com.mirror.product.dto.productionorder.GenerateOrdersRequest;
import com.mirror.product.dto.productionorder.GenerateOrdersResponse;
import com.mirror.product.dto.productionorder.ProductionOrderListResponse;
import com.mirror.product.dto.workflow.*;
import com.mirror.product.entity.CollectionPlan;
import com.mirror.product.entity.ProductionOrder;
import com.mirror.product.entity.ProductionPlan;
import com.mirror.product.enums.ProductionPlanStatus;
import com.mirror.product.mapper.ProductionOrderMapper;
import com.mirror.product.mapper.ProductionPlanMapper;
import com.mirror.product.service.ProductionOrderService;
import com.mirror.product.service.ProductionPlanService;
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

import java.util.*;

/**
 * REST Controller for Production Plan operations
 */
@RestController
@RequestMapping("/api/v1/production-plans")
@RequiredArgsConstructor
@Slf4j
public class ProductionPlanController {

    private final ProductionPlanService planService;
    private final ProductionPlanMapper mapper;
    private final ProductionOrderService orderService;
    private final ProductionOrderMapper orderMapper;

    /**
     * Get all production plans with pagination and filters
     * GET /api/v1/production-plans
     */
    @GetMapping
    public ResponseEntity<?> getAllPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProductionPlanStatus status,
            @RequestParam(required = false) UUID collectionPlanId) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            ProductionPlanSearchCriteria criteria = ProductionPlanSearchCriteria.builder()
                    .search(search)
                    .status(status)
                    .collectionPlanId(collectionPlanId)
                    .build();

            Page<ProductionPlan> planPage = planService.search(criteria, pageable);

            // Enrich with collection plan names
            List<ProductionPlanListResponse> responses = new ArrayList<>();
            for (ProductionPlan plan : planPage.getContent()) {
                ProductionPlanListResponse response = mapper.toListResponse(plan);
                // Get collection plan name if exists
                if (plan.getCollectionPlanId() != null) {
                    planService.getCollectionPlan(plan.getId())
                            .ifPresent(cp -> response.setCollectionPlanName(cp.getName()));
                }
                responses.add(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("content", responses);
            response.put("currentPage", planPage.getNumber());
            response.put("totalItems", planPage.getTotalElements());
            response.put("totalPages", planPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting production plans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching production plans");
        }
    }

    /**
     * Get production plan by ID with details
     * GET /api/v1/production-plans/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPlanById(@PathVariable String id) {
        try {
            Optional<ProductionPlan> planOpt = planService.findByIdWithWorkflowTemplate(id);
            if (planOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ProductionPlan plan = planOpt.get();
            ProductionPlanResponse response = mapper.toResponseWithTemplate(plan);

            // Add collection plan name if exists
            if (plan.getCollectionPlanId() != null) {
                planService.getCollectionPlan(id)
                        .ifPresent(cp -> response.setCollectionPlanName(cp.getName()));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting production plan by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching production plan");
        }
    }

    /**
     * Create a new production plan
     * POST /api/v1/production-plans
     */
    @PostMapping
    public ResponseEntity<?> createPlan(
            @Valid @RequestBody ProductionPlanCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionPlan plan = planService.create(request, userId);
            ProductionPlanResponse response = mapper.toResponse(plan);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid request for creating production plan: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            log.error("Error creating production plan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating production plan");
        } catch (Exception e) {
            log.error("Error creating production plan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating production plan");
        }
    }

    /**
     * Create a production plan from a collection plan
     * POST /api/v1/production-plans/from-collection-plan
     */
    @PostMapping("/from-collection-plan")
    public ResponseEntity<?> createFromCollectionPlan(
            @Valid @RequestBody CreateFromCollectionPlanRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionPlan plan = planService.createFromCollectionPlan(request, userId);

            // Get collection plan for response enrichment
            Optional<CollectionPlan> collectionPlan = planService.getCollectionPlan(plan.getId());
            ProductionPlanResponse response = collectionPlan
                    .map(cp -> mapper.toResponseWithCollectionPlan(plan, cp))
                    .orElse(mapper.toResponse(plan));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid request for creating production plan from collection plan: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            log.error("Error creating production plan from collection plan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating production plan");
        } catch (Exception e) {
            log.error("Error creating production plan from collection plan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating production plan");
        }
    }

    /**
     * Update an existing production plan
     * PUT /api/v1/production-plans/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(
            @PathVariable String id,
            @Valid @RequestBody ProductionPlanUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionPlan plan = planService.update(id, request, userId);
            ProductionPlanResponse response = mapper.toResponse(plan);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error updating production plan {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating production plan: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating production plan");
        }
    }

    /**
     * Soft delete a production plan
     * DELETE /api/v1/production-plans/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlan(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            planService.softDelete(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot delete production plan {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting production plan: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while deleting production plan");
        }
    }

    /**
     * Update production plan status
     * PATCH /api/v1/production-plans/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updatePlanStatus(
            @PathVariable String id,
            @Valid @RequestBody ProductionPlanStatusUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionPlan plan = planService.updateStatus(id, request.getStatus(), userId, request.getReason());
            ProductionPlanResponse response = mapper.toResponse(plan);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error updating production plan status {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating production plan status: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating production plan status");
        }
    }

    /**
     * Get production plans by status
     * GET /api/v1/production-plans/by-status/{status}
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<?> getPlansByStatus(@PathVariable ProductionPlanStatus status) {
        try {
            List<ProductionPlan> plans = planService.findByStatus(status);
            List<ProductionPlanListResponse> responses = mapper.toListResponseList(plans);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting production plans by status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching production plans");
        }
    }

    /**
     * Get production plans by collection plan
     * GET /api/v1/production-plans/by-collection-plan/{collectionPlanId}
     */
    @GetMapping("/by-collection-plan/{collectionPlanId}")
    public ResponseEntity<?> getPlansByCollectionPlan(@PathVariable UUID collectionPlanId) {
        try {
            List<ProductionPlan> plans = planService.findByCollectionPlanId(collectionPlanId);
            List<ProductionPlanListResponse> responses = mapper.toListResponseList(plans);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting production plans by collection plan: {}", collectionPlanId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching production plans");
        }
    }

    /**
     * Get all active production plans (APPROVED or IN_PRODUCTION)
     * GET /api/v1/production-plans/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActivePlans() {
        try {
            List<ProductionPlan> plans = planService.findAllActivePlans();
            List<ProductionPlanListResponse> responses = mapper.toListResponseList(plans);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting active production plans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching active production plans");
        }
    }

    /**
     * Get production plan count
     * GET /api/v1/production-plans/count
     */
    @GetMapping("/count")
    public ResponseEntity<?> getPlanCount() {
        try {
            long count = planService.countActive();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error getting production plan count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching production plan count");
        }
    }

    /**
     * Check if a collection plan has any production plans
     * GET /api/v1/production-plans/exists-for-collection-plan/{collectionPlanId}
     */
    @GetMapping("/exists-for-collection-plan/{collectionPlanId}")
    public ResponseEntity<?> hasProductionPlans(@PathVariable UUID collectionPlanId) {
        try {
            boolean exists = planService.hasProductionPlans(collectionPlanId);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            log.error("Error checking production plans for collection plan: {}", collectionPlanId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while checking production plans");
        }
    }

    // ==================== Approval Flow (P3-09) ====================

    /**
     * Submit a plan for approval
     * POST /api/v1/production-plans/{id}/submit-for-approval
     */
    @PostMapping("/{id}/submit-for-approval")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_ADMIN', 'SUPER_ADMIN', 'PRODUCTION_OPS', 'CSO')")
    public ResponseEntity<?> submitForApproval(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionPlan plan = planService.submitForApproval(id, userId);
            ProductionPlanResponse response = mapper.toResponse(plan);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error submitting plan for approval {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Approve a production plan
     * POST /api/v1/production-plans/{id}/approve
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_ADMIN', 'SUPER_ADMIN', 'CSO')")
    public ResponseEntity<?> approvePlan(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean forceApprove,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Map<String, Object> result = planService.approvePlan(id, userId, forceApprove);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error approving plan {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Reject a production plan
     * POST /api/v1/production-plans/{id}/reject
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_ADMIN', 'SUPER_ADMIN', 'CSO')")
    public ResponseEntity<?> rejectPlan(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionPlan plan = planService.rejectPlan(id, userId, reason != null ? reason : "No reason provided");
            ProductionPlanResponse response = mapper.toResponse(plan);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error rejecting plan {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Start production for a plan
     * POST /api/v1/production-plans/{id}/start-production
     */
    @PostMapping("/{id}/start-production")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_ADMIN', 'SUPER_ADMIN', 'PRODUCTION_OPS', 'CSO')")
    public ResponseEntity<?> startProduction(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionPlan plan = planService.startProduction(id, userId);
            ProductionPlanResponse response = mapper.toResponse(plan);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error starting production for plan {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Complete a production plan
     * POST /api/v1/production-plans/{id}/complete
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_ADMIN', 'SUPER_ADMIN', 'PRODUCTION_OPS', 'CSO')")
    public ResponseEntity<?> completePlan(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionPlan plan = planService.completePlan(id, userId);
            ProductionPlanResponse response = mapper.toResponse(plan);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error completing plan {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get approval readiness status for a plan
     * GET /api/v1/production-plans/{id}/approval-readiness
     */
    @GetMapping("/{id}/approval-readiness")
    public ResponseEntity<?> getApprovalReadiness(@PathVariable String id) {
        try {
            Map<String, Object> readiness = planService.getApprovalReadiness(id);
            return ResponseEntity.ok(readiness);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error getting approval readiness for plan {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while checking approval readiness");
        }
    }

    // ==================== Production Order Integration ====================

    /**
     * Generate production orders from a plan
     * POST /api/v1/production-plans/{id}/generate-orders
     */
    @PostMapping("/{id}/generate-orders")
    public ResponseEntity<?> generateOrders(
            @PathVariable String id,
            @RequestBody(required = false) GenerateOrdersRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            GenerateOrdersResponse response = orderService.generateOrdersFromPlan(id, request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Cannot generate orders from plan {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error generating orders from plan: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while generating orders");
        }
    }

    /**
     * Get orders for a production plan
     * GET /api/v1/production-plans/{planId}/orders
     */
    @GetMapping("/{planId}/orders")
    public ResponseEntity<?> getOrdersForPlan(@PathVariable String planId) {
        try {
            // Verify plan exists
            if (!planService.existsById(planId)) {
                return ResponseEntity.notFound().build();
            }

            List<ProductionOrder> orders = orderService.findByProductionPlanId(planId);
            List<ProductionOrderListResponse> responses = orderMapper.toListResponseList(orders);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting orders for plan: {}", planId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching orders");
        }
    }

    /**
     * Get order count for a plan
     * GET /api/v1/production-plans/{planId}/orders/count
     */
    @GetMapping("/{planId}/orders/count")
    public ResponseEntity<?> getOrderCountForPlan(@PathVariable String planId) {
        try {
            // Verify plan exists
            if (!planService.existsById(planId)) {
                return ResponseEntity.notFound().build();
            }

            int count = orderService.countByProductionPlanId(planId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error getting order count for plan: {}", planId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching order count");
        }
    }
}
