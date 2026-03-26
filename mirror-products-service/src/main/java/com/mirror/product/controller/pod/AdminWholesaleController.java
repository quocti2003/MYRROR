package com.mirror.product.controller.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.enums.WholesaleOrderStatus;
import com.mirror.product.service.pod.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Wholesale Controller - admin endpoints for managing franchise partners and wholesale orders.
 */
@RestController
@RequestMapping("/api/v1/admin/wholesale")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'IT_ADMIN', 'SUPER_ADMIN')")
public class AdminWholesaleController {

    private final WholesaleOrderService wholesaleOrderService;
    private final PartnerInventoryService inventoryService;
    private final PartnerSaleService saleService;
    private final PhygitalPartnerPortalService portalService;

    // ==================== WHOLESALE ORDER MANAGEMENT ====================

    @GetMapping("/orders")
    public ResponseEntity<Page<WholesaleOrderResponse>> searchOrders(
            @RequestParam(required = false) WholesaleOrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(wholesaleOrderService.searchOrders(status, pageable));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<WholesaleOrderResponse> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(wholesaleOrderService.getOrderAdmin(id));
    }

    @PostMapping("/orders/{id}/approve")
    public ResponseEntity<WholesaleOrderResponse> approveOrder(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(wholesaleOrderService.approveOrder(id, authentication.getName()));
    }

    @PostMapping("/orders/{id}/process")
    public ResponseEntity<WholesaleOrderResponse> processOrder(@PathVariable String id) {
        return ResponseEntity.ok(wholesaleOrderService.processOrder(id));
    }

    @PostMapping("/orders/{id}/ship")
    public ResponseEntity<WholesaleOrderResponse> shipOrder(
            @PathVariable String id,
            @RequestParam @NotBlank @Size(max = 100) String trackingNumber) {
        return ResponseEntity.ok(wholesaleOrderService.shipOrder(id, trackingNumber));
    }

    @PostMapping("/orders/{id}/complete")
    public ResponseEntity<WholesaleOrderResponse> completeOrder(@PathVariable String id) {
        return ResponseEntity.ok(wholesaleOrderService.completeOrder(id));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<WholesaleOrderResponse> cancelOrder(
            @PathVariable String id,
            @RequestParam(required = false) @Size(max = 1000) String reason,
            Authentication authentication) {
        return ResponseEntity.ok(wholesaleOrderService.adminCancelOrder(id, reason, authentication.getName()));
    }

    // ==================== FRANCHISE PARTNER OVERVIEW ====================

    @GetMapping("/partners")
    public ResponseEntity<Page<PartnerResponse>> getPhygitalPartners(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(portalService.getPhygitalPartners(pageable));
    }

    @PostMapping("/partners/{partnerId}/recalculate-prices")
    public ResponseEntity<?> recalculateWholesalePrices(@PathVariable String partnerId) {
        int inventoryUpdated = inventoryService.recalculateWholesalePrices(partnerId);
        int salesUpdated = saleService.recalculateSaleProfits(partnerId);
        return ResponseEntity.ok(java.util.Map.of(
                "partnerId", partnerId,
                "inventoryUpdated", inventoryUpdated,
                "salesUpdated", salesUpdated));
    }

    @GetMapping("/partners/{partnerId}/inventory")
    public ResponseEntity<Page<PartnerInventoryResponse>> getPartnerInventory(
            @PathVariable String partnerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getInventory(partnerId, pageable));
    }

    @GetMapping("/partners/{partnerId}/sales")
    public ResponseEntity<Page<PartnerSaleResponse>> getPartnerSales(
            @PathVariable String partnerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(saleService.getSalesByPartner(partnerId, pageable));
    }

    @GetMapping("/partners/{partnerId}/orders")
    public ResponseEntity<Page<WholesaleOrderResponse>> getPartnerOrders(
            @PathVariable String partnerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(wholesaleOrderService.getOrdersByPartner(partnerId, pageable));
    }

    @GetMapping("/partners/{partnerId}/dashboard")
    public ResponseEntity<PhygitalDashboardResponse> getPartnerDashboard(@PathVariable String partnerId) {
        return ResponseEntity.ok(portalService.getDashboard(partnerId));
    }
}
