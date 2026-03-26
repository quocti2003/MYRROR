package com.mirror.product.controller;

import com.mirror.product.dto.PurchaseOrderItemRequest;
import com.mirror.product.dto.PurchaseOrderRequest;
import com.mirror.product.dto.PurchaseOrderResponse;
import com.mirror.product.entity.MaterialInventory;
import com.mirror.product.entity.PurchaseOrder;
import com.mirror.product.entity.PurchaseOrderItem;
import com.mirror.product.entity.Vendor;
import com.mirror.product.service.MaterialInventoryService;
import com.mirror.product.service.PurchaseOrderService;
import com.mirror.product.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for Purchase Order management - Procurement system
 * Migrated from mirror-mrp-service
 */
@RestController
@RequestMapping("/api/purchase-orders")
@CrossOrigin(origins = "*")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private MaterialInventoryService materialInventoryService;

    /**
     * Get all purchase orders
     * GET /api/purchase-orders
     */
    @GetMapping
    public ResponseEntity<List<PurchaseOrderResponse>> getAllPurchaseOrders(
            @RequestParam(value = "includeItems", defaultValue = "true") boolean includeItems) {
        List<PurchaseOrder> purchaseOrders = purchaseOrderService.findAll();
        List<PurchaseOrderResponse> response = purchaseOrders.stream()
                .map(po -> new PurchaseOrderResponse(po, includeItems))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get purchase order by ID
     * GET /api/purchase-orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> getPurchaseOrderById(
            @PathVariable UUID id,
            @RequestParam(value = "includeItems", defaultValue = "true") boolean includeItems) {
        return purchaseOrderService.findById(id)
                .map(po -> ResponseEntity.ok(new PurchaseOrderResponse(po, includeItems)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get purchase orders by status
     * GET /api/purchase-orders/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseOrderResponse>> getPurchaseOrdersByStatus(@PathVariable String status) {
        List<PurchaseOrder> purchaseOrders = purchaseOrderService.findByStatus(status);
        List<PurchaseOrderResponse> response = purchaseOrders.stream()
                .map(po -> new PurchaseOrderResponse(po, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get purchase orders by vendor
     * GET /api/purchase-orders/vendor/{vendorId}
     */
    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<PurchaseOrderResponse>> getPurchaseOrdersByVendor(
            @PathVariable String vendorId,
            @RequestParam(required = false) String status) {
        List<PurchaseOrder> purchaseOrders;
        if (status != null) {
            purchaseOrders = purchaseOrderService.findByVendorIdAndStatus(vendorId, status);
        } else {
            purchaseOrders = purchaseOrderService.findByVendorId(vendorId);
        }
        List<PurchaseOrderResponse> response = purchaseOrders.stream()
                .map(po -> new PurchaseOrderResponse(po, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get purchase orders by expected delivery date range
     * GET /api/purchase-orders/delivery-date?start={start}&end={end}
     */
    @GetMapping("/delivery-date")
    public ResponseEntity<List<PurchaseOrderResponse>> getPurchaseOrdersByDeliveryDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        List<PurchaseOrder> purchaseOrders = purchaseOrderService.findByExpectedDeliveryDateBetween(start, end);
        List<PurchaseOrderResponse> response = purchaseOrders.stream()
                .map(po -> new PurchaseOrderResponse(po, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Create new purchase order
     * POST /api/purchase-orders
     */
    @PostMapping
    public ResponseEntity<?> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        try {
            // Build purchase order entity from request
            Vendor vendor = vendorService.findActiveById(request.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + request.getVendorId()));

            // Build items
            Set<PurchaseOrderItem> items = request.getItems().stream()
                    .map(itemRequest -> {
                        MaterialInventory material = materialInventoryService.findById(itemRequest.getMaterialId())
                                .orElseThrow(() -> new IllegalArgumentException("Material not found with id: " + itemRequest.getMaterialId()));

                        return PurchaseOrderItem.builder()
                                .material(material)
                                .quantity(itemRequest.getQuantity())
                                .unitPrice(itemRequest.getUnitPrice())
                                .build();
                    })
                    .collect(Collectors.toSet());

            PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                    .vendor(vendor)
                    .status(request.getStatus())
                    .expectedDeliveryDate(request.getExpectedDeliveryDate())
                    .poItems(items)
                    .build();

            PurchaseOrder created = purchaseOrderService.create(purchaseOrder);
            PurchaseOrderResponse response = new PurchaseOrderResponse(created, true);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update existing purchase order
     * PUT /api/purchase-orders/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePurchaseOrder(
            @PathVariable UUID id,
            @Valid @RequestBody PurchaseOrderRequest request) {
        try {
            // Build purchase order entity from request
            Vendor vendor = vendorService.findActiveById(request.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + request.getVendorId()));

            // Build items
            Set<PurchaseOrderItem> items = request.getItems().stream()
                    .map(itemRequest -> {
                        MaterialInventory material = materialInventoryService.findById(itemRequest.getMaterialId())
                                .orElseThrow(() -> new IllegalArgumentException("Material not found with id: " + itemRequest.getMaterialId()));

                        return PurchaseOrderItem.builder()
                                .material(material)
                                .quantity(itemRequest.getQuantity())
                                .unitPrice(itemRequest.getUnitPrice())
                                .build();
                    })
                    .collect(Collectors.toSet());

            PurchaseOrder purchaseOrderDetails = PurchaseOrder.builder()
                    .vendor(vendor)
                    .status(request.getStatus())
                    .expectedDeliveryDate(request.getExpectedDeliveryDate())
                    .poItems(items)
                    .build();

            PurchaseOrder updated = purchaseOrderService.update(id, purchaseOrderDetails);
            PurchaseOrderResponse response = new PurchaseOrderResponse(updated, true);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update purchase order status
     * PATCH /api/purchase-orders/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updatePurchaseOrderStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || status.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
            }

            PurchaseOrder updated = purchaseOrderService.updateStatus(id, status);
            PurchaseOrderResponse response = new PurchaseOrderResponse(updated, false);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cancel purchase order
     * POST /api/purchase-orders/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelPurchaseOrder(@PathVariable UUID id) {
        try {
            PurchaseOrder cancelled = purchaseOrderService.cancel(id);
            PurchaseOrderResponse response = new PurchaseOrderResponse(cancelled, false);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete purchase order
     * DELETE /api/purchase-orders/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePurchaseOrder(@PathVariable UUID id) {
        try {
            purchaseOrderService.deleteById(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Purchase order deleted successfully",
                    "id", id.toString()
            ));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get purchase order count
     * GET /api/purchase-orders/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getPurchaseOrderCount() {
        long count = purchaseOrderService.count();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
