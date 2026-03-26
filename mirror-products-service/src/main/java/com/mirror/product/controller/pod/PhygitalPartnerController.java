package com.mirror.product.controller.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.service.pod.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Phygital Partner Controller - endpoints for franchise partners.
 * Manages inventory, wholesale orders, sales, and dashboard.
 */
@RestController
@RequestMapping("/api/v1/partner/phygital")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('PARTNER')")
public class PhygitalPartnerController {

    private final PartnerInventoryService inventoryService;
    private final WholesaleOrderService wholesaleOrderService;
    private final PartnerSaleService saleService;
    private final PhygitalPartnerPortalService portalService;
    private final PartnerPortalService partnerPortalService;

    // ==================== INVENTORY ====================

    @GetMapping("/inventory")
    public ResponseEntity<Page<PartnerInventoryResponse>> getInventory(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(inventoryService.getInventory(partnerId, pageable));
    }

    @GetMapping("/inventory/{id}")
    public ResponseEntity<PartnerInventoryResponse> getInventoryItem(
            @PathVariable String id,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(inventoryService.getInventoryItem(partnerId, id));
    }

    @PutMapping("/inventory/{id}/price")
    public ResponseEntity<PartnerInventoryResponse> updateRetailPrice(
            @PathVariable String id,
            @Valid @RequestBody InventoryPriceUpdateRequest request,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(inventoryService.updateRetailPrice(partnerId, id, request));
    }

    @PostMapping("/inventory/{id}/adjust")
    public ResponseEntity<PartnerInventoryResponse> adjustStock(
            @PathVariable String id,
            @Valid @RequestBody InventoryAdjustRequest request,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(inventoryService.adjustStock(partnerId, id, request, authentication.getName()));
    }

    @GetMapping("/inventory/low-stock")
    public ResponseEntity<List<PartnerInventoryResponse>> getLowStockAlerts(Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(inventoryService.getLowStockAlerts(partnerId));
    }

    @GetMapping("/inventory/{id}/movements")
    public ResponseEntity<Page<InventoryMovementResponse>> getMovementHistory(
            @PathVariable String id,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(inventoryService.getMovementHistory(partnerId, id, pageable));
    }

    // ==================== WHOLESALE ORDERS ====================

    @GetMapping("/wholesale-orders")
    public ResponseEntity<Page<WholesaleOrderResponse>> getOrders(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(wholesaleOrderService.getOrdersByPartner(partnerId, pageable));
    }

    @GetMapping("/wholesale-orders/{id}")
    public ResponseEntity<WholesaleOrderResponse> getOrder(
            @PathVariable String id,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(wholesaleOrderService.getOrder(partnerId, id));
    }

    @PostMapping("/wholesale-orders")
    public ResponseEntity<WholesaleOrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody WholesaleOrderCreateRequest request) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(wholesaleOrderService.createOrder(partnerId, request));
    }

    @PostMapping("/wholesale-orders/{id}/submit")
    public ResponseEntity<WholesaleOrderResponse> submitOrder(
            @PathVariable String id,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(wholesaleOrderService.submitOrder(partnerId, id));
    }

    @PostMapping("/wholesale-orders/{id}/cancel")
    public ResponseEntity<WholesaleOrderResponse> cancelOrder(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(wholesaleOrderService.cancelOrder(partnerId, id, reason, authentication.getName()));
    }

    @PostMapping("/wholesale-orders/{id}/confirm-delivery")
    public ResponseEntity<WholesaleOrderResponse> confirmDelivery(
            @PathVariable String id,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(wholesaleOrderService.confirmDelivery(partnerId, id));
    }

    // ==================== SALES ====================

    @GetMapping("/sales")
    public ResponseEntity<Page<PartnerSaleResponse>> getSales(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(saleService.getSalesByPartner(partnerId, pageable));
    }

    @GetMapping("/sales/{id}")
    public ResponseEntity<PartnerSaleResponse> getSale(
            @PathVariable String id,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(saleService.getSale(partnerId, id));
    }

    @PostMapping("/sales")
    public ResponseEntity<PartnerSaleResponse> recordSale(
            Authentication authentication,
            @Valid @RequestBody PartnerSaleCreateRequest request) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.recordSale(partnerId, request));
    }

    @PostMapping("/sales/{id}/confirm")
    public ResponseEntity<PartnerSaleResponse> confirmSale(
            @PathVariable String id,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(saleService.confirmSale(partnerId, id));
    }

    @PostMapping("/sales/{id}/complete")
    public ResponseEntity<PartnerSaleResponse> completeSale(
            @PathVariable String id,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(saleService.completeSale(partnerId, id));
    }

    @PostMapping("/sales/{id}/cancel")
    public ResponseEntity<PartnerSaleResponse> cancelSale(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(saleService.cancelSale(partnerId, id, reason));
    }

    @PostMapping("/sales/{id}/return")
    public ResponseEntity<PartnerSaleResponse> returnSale(
            @PathVariable String id,
            Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(saleService.returnSale(partnerId, id));
    }

    // ==================== DASHBOARD & REPORTS ====================

    @GetMapping("/dashboard")
    public ResponseEntity<PhygitalDashboardResponse> getDashboard(Authentication authentication) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(portalService.getDashboard(partnerId));
    }

    @GetMapping("/reports/sales")
    public ResponseEntity<PartnerSaleService.SalesStatistics> getSalesReport(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String partnerId = getPartnerId(authentication);
        return ResponseEntity.ok(saleService.getSalesStatistics(partnerId, startDate, endDate));
    }

    // === HELPER ===

    private String getPartnerId(Authentication authentication) {
        String username = authentication.getName();
        PartnerProfileResponse profile = partnerPortalService.getPartnerProfileByUsername(username);
        return profile.getId();
    }
}
