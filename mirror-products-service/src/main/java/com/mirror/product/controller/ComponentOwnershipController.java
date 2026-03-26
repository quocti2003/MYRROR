package com.mirror.product.controller;

import com.mirror.product.dto.componentownership.*;
import com.mirror.product.enums.HandoffStatus;
import com.mirror.product.service.ComponentOwnershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Component Ownership tracking.
 *
 * Provides endpoints for:
 * - Viewing ownership history for production orders
 * - Initiating, tracking, and confirming component handoffs
 * - Viewing pending receipts for vendors
 * - Managing overdue handoffs
 */
@RestController
@RequestMapping("/api/v1/component-ownership")
@RequiredArgsConstructor
@Slf4j
public class ComponentOwnershipController {

    private final ComponentOwnershipService ownershipService;

    // ==================== Query Endpoints ====================

    /**
     * Get handoff by ID
     * GET /api/v1/component-ownership/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            return ownershipService.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting handoff {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get complete ownership history for a production order
     * GET /api/v1/component-ownership/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getOrderHistory(@PathVariable String orderId) {
        try {
            List<ComponentOwnershipLogDTO> history = ownershipService.getOrderHistory(orderId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error getting order history for {}: {}", orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get paginated ownership history for a production order
     * GET /api/v1/component-ownership/order/{orderId}/paged
     */
    @GetMapping("/order/{orderId}/paged")
    public ResponseEntity<?> getOrderHistoryPaged(
            @PathVariable String orderId,
            @PageableDefault(size = 20, sort = "initiatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<ComponentOwnershipLogDTO> history = ownershipService.getOrderHistory(orderId, pageable);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error getting paged order history for {}: {}", orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get the most recent handoff for a production order
     * GET /api/v1/component-ownership/order/{orderId}/latest
     */
    @GetMapping("/order/{orderId}/latest")
    public ResponseEntity<?> getLatestHandoff(@PathVariable String orderId) {
        try {
            return ownershipService.getLatestHandoff(orderId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting latest handoff for {}: {}", orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get handoffs awaiting receipt confirmation by vendor
     * GET /api/v1/component-ownership/vendor/{vendorId}/pending
     */
    @GetMapping("/vendor/{vendorId}/pending")
    public ResponseEntity<?> getPendingReceipts(@PathVariable String vendorId) {
        try {
            List<ComponentOwnershipLogDTO> pending = ownershipService.getPendingReceipts(vendorId);
            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            log.error("Error getting pending receipts for vendor {}: {}", vendorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get paginated handoffs awaiting receipt confirmation
     * GET /api/v1/component-ownership/vendor/{vendorId}/pending/paged
     */
    @GetMapping("/vendor/{vendorId}/pending/paged")
    public ResponseEntity<?> getPendingReceiptsPaged(
            @PathVariable String vendorId,
            @PageableDefault(size = 20, sort = "initiatedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        try {
            Page<ComponentOwnershipLogDTO> pending = ownershipService.getPendingReceipts(vendorId, pageable);
            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            log.error("Error getting paged pending receipts for vendor {}: {}", vendorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get count of handoffs awaiting receipt confirmation
     * GET /api/v1/component-ownership/vendor/{vendorId}/pending/count
     */
    @GetMapping("/vendor/{vendorId}/pending/count")
    public ResponseEntity<?> countPendingReceipts(@PathVariable String vendorId) {
        try {
            long count = ownershipService.countPendingForVendor(vendorId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error counting pending receipts for vendor {}: {}", vendorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get items currently held by a vendor
     * GET /api/v1/component-ownership/vendor/{vendorId}/holding
     */
    @GetMapping("/vendor/{vendorId}/holding")
    public ResponseEntity<?> getItemsHeldByVendor(@PathVariable String vendorId) {
        try {
            List<ComponentOwnershipLogDTO> items = ownershipService.getItemsHeldByVendor(vendorId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("Error getting items held by vendor {}: {}", vendorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all handoffs initiated by a vendor
     * GET /api/v1/component-ownership/vendor/{vendorId}/sent
     */
    @GetMapping("/vendor/{vendorId}/sent")
    public ResponseEntity<?> getHandoffsSentByVendor(@PathVariable String vendorId) {
        try {
            List<ComponentOwnershipLogDTO> handoffs = ownershipService.getHandoffsSentByVendor(vendorId);
            return ResponseEntity.ok(handoffs);
        } catch (Exception e) {
            log.error("Error getting handoffs sent by vendor {}: {}", vendorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all handoffs received by a vendor
     * GET /api/v1/component-ownership/vendor/{vendorId}/received
     */
    @GetMapping("/vendor/{vendorId}/received")
    public ResponseEntity<?> getHandoffsReceivedByVendor(@PathVariable String vendorId) {
        try {
            List<ComponentOwnershipLogDTO> handoffs = ownershipService.getHandoffsReceivedByVendor(vendorId);
            return ResponseEntity.ok(handoffs);
        } catch (Exception e) {
            log.error("Error getting handoffs received by vendor {}: {}", vendorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all handoffs past expected arrival date
     * GET /api/v1/component-ownership/overdue
     */
    @GetMapping("/overdue")
    public ResponseEntity<?> getOverdueHandoffs() {
        try {
            List<ComponentOwnershipLogDTO> overdue = ownershipService.getOverdueHandoffs();
            return ResponseEntity.ok(overdue);
        } catch (Exception e) {
            log.error("Error getting overdue handoffs: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get handoffs past expected arrival date for a vendor
     * GET /api/v1/component-ownership/vendor/{vendorId}/overdue
     */
    @GetMapping("/vendor/{vendorId}/overdue")
    public ResponseEntity<?> getOverdueHandoffsForVendor(@PathVariable String vendorId) {
        try {
            List<ComponentOwnershipLogDTO> overdue = ownershipService.getOverdueHandoffsForVendor(vendorId);
            return ResponseEntity.ok(overdue);
        } catch (Exception e) {
            log.error("Error getting overdue handoffs for vendor {}: {}", vendorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all handoffs with a specific status
     * GET /api/v1/component-ownership/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getByStatus(
            @PathVariable HandoffStatus status,
            @PageableDefault(size = 20, sort = "initiatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<ComponentOwnershipLogDTO> handoffs = ownershipService.findByStatus(status, pageable);
            return ResponseEntity.ok(handoffs);
        } catch (Exception e) {
            log.error("Error getting handoffs by status {}: {}", status, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if order has an active handoff in progress
     * GET /api/v1/component-ownership/order/{orderId}/has-active
     */
    @GetMapping("/order/{orderId}/has-active")
    public ResponseEntity<?> hasActiveHandoff(@PathVariable String orderId) {
        try {
            boolean hasActive = ownershipService.hasActiveHandoff(orderId);
            return ResponseEntity.ok(Map.of("hasActiveHandoff", hasActive));
        } catch (Exception e) {
            log.error("Error checking active handoff for {}: {}", orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Handoff Operations ====================

    /**
     * Initiate a new component handoff
     * POST /api/v1/component-ownership/initiate
     */
    @PostMapping("/initiate")
    public ResponseEntity<?> initiateHandoff(
            @Valid @RequestBody InitiateHandoffRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = userDetails != null ? userDetails.getUsername() : "system";
            ComponentOwnershipLogDTO result = ownershipService.initiateHandoff(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalStateException e) {
            log.warn("Cannot initiate handoff: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error initiating handoff: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error initiating handoff: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Mark handoff as in-transit with shipping details
     * PATCH /api/v1/component-ownership/{id}/in-transit
     */
    @PatchMapping("/{id}/in-transit")
    public ResponseEntity<?> markInTransit(
            @PathVariable String id,
            @Valid @RequestBody MarkInTransitRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = userDetails != null ? userDetails.getUsername() : "system";
            ComponentOwnershipLogDTO result = ownershipService.markInTransit(id, request, userId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Cannot mark in-transit {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error marking in-transit {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error marking in-transit {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Confirm receipt of a handoff
     * PATCH /api/v1/component-ownership/{id}/confirm-receipt
     */
    @PatchMapping("/{id}/confirm-receipt")
    public ResponseEntity<?> confirmReceipt(
            @PathVariable String id,
            @RequestBody(required = false) ConfirmReceiptRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = userDetails != null ? userDetails.getUsername() : "system";
            ComponentOwnershipLogDTO result = ownershipService.confirmReceipt(id, request, userId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Cannot confirm receipt for {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error confirming receipt for {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error confirming receipt for {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reject a handoff with reason
     * PATCH /api/v1/component-ownership/{id}/reject
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectHandoff(
            @PathVariable String id,
            @Valid @RequestBody RejectHandoffRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = userDetails != null ? userDetails.getUsername() : "system";
            ComponentOwnershipLogDTO result = ownershipService.rejectHandoff(id, request, userId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Cannot reject handoff {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error rejecting handoff {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error rejecting handoff {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cancel an initiated handoff
     * PATCH /api/v1/component-ownership/{id}/cancel
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelHandoff(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = userDetails != null ? userDetails.getUsername() : "system";
            ComponentOwnershipLogDTO result = ownershipService.cancelHandoff(id, reason, userId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Cannot cancel handoff {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error cancelling handoff {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error cancelling handoff {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
