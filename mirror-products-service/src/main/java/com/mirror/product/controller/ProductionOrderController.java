package com.mirror.product.controller;

import com.mirror.product.dto.productionorder.*;
import com.mirror.product.entity.ProductionOrder;
import com.mirror.product.entity.ProductionOrderStage;
import com.mirror.product.enums.ProductionOrderStatus;
import com.mirror.product.mapper.ProductionOrderMapper;
import com.mirror.product.service.ProductionOrderService;
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

/**
 * REST Controller for Production Order operations
 */
@RestController
@RequestMapping("/api/v1/production-orders")
@RequiredArgsConstructor
@Slf4j
public class ProductionOrderController {

    private final ProductionOrderService orderService;
    private final ProductionOrderMapper mapper;

    // ==================== Order CRUD Operations ====================

    /**
     * Get all production orders with pagination and filters
     * GET /api/v1/production-orders
     */
    @GetMapping
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String productionPlanId,
            @RequestParam(required = false) ProductionOrderStatus status,
            @RequestParam(required = false) String vendorId) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            ProductionOrderSearchCriteria criteria = ProductionOrderSearchCriteria.builder()
                    .search(search)
                    .productionPlanId(productionPlanId)
                    .status(status)
                    .vendorId(vendorId)
                    .build();

            Page<ProductionOrder> orderPage = orderService.search(criteria, pageable);
            List<ProductionOrderListResponse> responses = mapper.toListResponseList(orderPage.getContent());

            Map<String, Object> response = new HashMap<>();
            response.put("content", responses);
            response.put("currentPage", orderPage.getNumber());
            response.put("totalItems", orderPage.getTotalElements());
            response.put("totalPages", orderPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting production orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching production orders");
        }
    }

    /**
     * Get production order by ID with stages
     * GET /api/v1/production-orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable String id) {
        try {
            Optional<ProductionOrder> orderOpt = orderService.findByIdWithStages(id);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ProductionOrderResponse response = mapper.toResponseWithStages(orderOpt.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting production order by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching production order");
        }
    }

    /**
     * Create a new production order
     * POST /api/v1/production-orders
     */
    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody ProductionOrderCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionOrder order = orderService.create(request, userId);
            ProductionOrderResponse response = mapper.toResponseWithStages(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid request for creating production order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            log.error("Error creating production order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating production order");
        }
    }

    /**
     * Update an existing production order
     * PUT /api/v1/production-orders/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(
            @PathVariable String id,
            @Valid @RequestBody ProductionOrderUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionOrder order = orderService.update(id, request, userId);
            ProductionOrderResponse response = mapper.toResponse(order);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error updating production order {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Soft delete a production order
     * DELETE /api/v1/production-orders/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            orderService.softDelete(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot delete production order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Update production order status
     * PATCH /api/v1/production-orders/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable String id,
            @Valid @RequestBody ProductionOrderStatusUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionOrder order = orderService.updateStatus(id, request.getStatus(), userId, request.getReason());
            ProductionOrderResponse response = mapper.toResponse(order);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error updating production order status {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==================== Stage Lifecycle Operations ====================

    /**
     * Start a stage (READY -> IN_PROGRESS)
     * POST /api/v1/production-orders/{orderId}/stages/{stageId}/start
     */
    @PostMapping("/{orderId}/stages/{stageId}/start")
    public ResponseEntity<?> startStage(
            @PathVariable String orderId,
            @PathVariable String stageId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionOrderStage stage = orderService.startStage(orderId, stageId, userId);
            ProductionOrderStageDTO response = mapper.toStageDTO(stage);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Cannot start stage {}: {}", stageId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error starting stage {}", stageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while starting stage");
        }
    }

    /**
     * Complete a stage (IN_PROGRESS -> COMPLETED, next stage -> READY)
     * POST /api/v1/production-orders/{orderId}/stages/{stageId}/complete
     */
    @PostMapping("/{orderId}/stages/{stageId}/complete")
    public ResponseEntity<?> completeStage(
            @PathVariable String orderId,
            @PathVariable String stageId,
            @RequestBody(required = false) StageCompleteRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionOrderStage stage = orderService.completeStage(orderId, stageId, request, userId);
            ProductionOrderStageDTO response = mapper.toStageDTO(stage);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Cannot complete stage {}: {}", stageId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error completing stage {}", stageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while completing stage");
        }
    }

    /**
     * Skip an optional stage
     * POST /api/v1/production-orders/{orderId}/stages/{stageId}/skip
     */
    @PostMapping("/{orderId}/stages/{stageId}/skip")
    public ResponseEntity<?> skipStage(
            @PathVariable String orderId,
            @PathVariable String stageId,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionOrderStage stage = orderService.skipStage(orderId, stageId, userId, reason);
            ProductionOrderStageDTO response = mapper.toStageDTO(stage);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Cannot skip stage {}: {}", stageId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error skipping stage {}", stageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while skipping stage");
        }
    }

    /**
     * Assign vendor to a stage
     * PATCH /api/v1/production-orders/{orderId}/stages/{stageId}/assign
     */
    @PatchMapping("/{orderId}/stages/{stageId}/assign")
    public ResponseEntity<?> assignVendorToStage(
            @PathVariable String orderId,
            @PathVariable String stageId,
            @Valid @RequestBody StageAssignVendorRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            ProductionOrderStage stage = orderService.assignVendorToStage(orderId, stageId, request, userId);
            ProductionOrderStageDTO response = mapper.toStageDTO(stage);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Cannot assign vendor to stage {}: {}", stageId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error assigning vendor to stage {}", stageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while assigning vendor");
        }
    }

    // ==================== Additional Query Endpoints ====================

    /**
     * Get orders by production plan
     * GET /api/v1/production-plans/{planId}/orders
     */
    @GetMapping("/by-plan/{planId}")
    public ResponseEntity<?> getOrdersByPlan(@PathVariable String planId) {
        try {
            List<ProductionOrder> orders = orderService.findByProductionPlanId(planId);
            List<ProductionOrderListResponse> responses = mapper.toListResponseList(orders);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting orders by plan: {}", planId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching orders");
        }
    }

    /**
     * Get orders by status
     * GET /api/v1/production-orders/by-status/{status}
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<?> getOrdersByStatus(@PathVariable ProductionOrderStatus status) {
        try {
            List<ProductionOrder> orders = orderService.findByStatus(status);
            List<ProductionOrderListResponse> responses = mapper.toListResponseList(orders);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting orders by status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching orders");
        }
    }

    /**
     * Get orders currently held by a vendor
     * GET /api/v1/production-orders/by-holder/{vendorId}
     */
    @GetMapping("/by-holder/{vendorId}")
    public ResponseEntity<?> getOrdersByHolder(@PathVariable String vendorId) {
        try {
            List<ProductionOrder> orders = orderService.findByCurrentHolderId(vendorId);
            List<ProductionOrderListResponse> responses = mapper.toListResponseList(orders);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting orders by holder: {}", vendorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching orders");
        }
    }

    /**
     * Get order count
     * GET /api/v1/production-orders/count
     */
    @GetMapping("/count")
    public ResponseEntity<?> getOrderCount() {
        try {
            long count = orderService.countActive();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error getting order count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching order count");
        }
    }

    /**
     * Get stages assigned to a vendor
     * GET /api/v1/production-orders/stages/by-vendor/{vendorId}
     */
    @GetMapping("/stages/by-vendor/{vendorId}")
    public ResponseEntity<?> getStagesByVendor(@PathVariable String vendorId) {
        try {
            List<ProductionOrderStage> stages = orderService.findStagesByVendorId(vendorId);
            List<ProductionOrderStageDTO> responses = mapper.toStageDTOList(stages);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting stages by vendor: {}", vendorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching stages");
        }
    }

    // ==================== Sprint 5: Partner Assignment Endpoints ====================

    /**
     * P3-03: Bulk assign vendors to multiple stages
     * POST /api/v1/production-orders/{orderId}/stages/bulk-assign
     */
    @PostMapping("/{orderId}/stages/bulk-assign")
    public ResponseEntity<?> bulkAssignVendors(
            @PathVariable String orderId,
            @Valid @RequestBody BulkAssignRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            BulkAssignResponse response = orderService.bulkAssignVendors(orderId, request, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error bulk assigning vendors to order {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error bulk assigning vendors to order {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while bulk assigning vendors");
        }
    }

    /**
     * P3-04: Get cost estimate for production order
     * GET /api/v1/production-orders/{orderId}/cost-estimate
     */
    @GetMapping("/{orderId}/cost-estimate")
    public ResponseEntity<?> getCostEstimate(@PathVariable String orderId) {
        try {
            CostEstimateResponse response = orderService.getCostEstimate(orderId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error getting cost estimate for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error getting cost estimate for order {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while calculating cost estimate");
        }
    }

    /**
     * P3-10: Get production orders by JTRC ID
     * GET /api/v1/production-orders/by-jtrc/{jtrcId}
     */
    @GetMapping("/by-jtrc/{jtrcId}")
    public ResponseEntity<?> getOrdersByJtrc(@PathVariable String jtrcId) {
        try {
            List<ProductionOrder> orders = orderService.findByJtrcId(jtrcId);
            List<ProductionOrderListResponse> responses = mapper.toListResponseList(orders);
            return ResponseEntity.ok(Map.of(
                    "jtrcId", jtrcId,
                    "totalOrders", responses.size(),
                    "orders", responses
            ));
        } catch (Exception e) {
            log.error("Error getting orders by JTRC: {}", jtrcId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching orders by JTRC");
        }
    }
}
