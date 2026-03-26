package com.mirror.product.controller;

import com.mirror.product.dto.OrderPaymentCollectionRequest;
import com.mirror.product.dto.OrderPaymentScheduleResponse;
import com.mirror.product.dto.OrderPaymentTermsUpdateRequest;
import com.mirror.product.dto.OrderPaymentTransactionResponse;
import com.mirror.product.dto.OrderRequest;
import com.mirror.product.dto.OrderResponse;
import com.mirror.product.dto.OrderStatusUpdateRequest;
import com.mirror.product.dto.OrderSummaryResponse;
import com.mirror.product.dto.OrderConfirmRequest;
import com.mirror.product.dto.OrderShipRequest;
import com.mirror.product.dto.OrderCompleteRequest;
import com.mirror.product.dto.OrderVendorAssignRequest;
import com.mirror.product.enums.OrderStatus;
import com.mirror.product.enums.PaymentScheduleStatus;
import com.mirror.product.enums.PaymentStatus;
import com.mirror.product.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public ResponseEntity<?> getOrders(@RequestParam(defaultValue = "false") boolean paginated,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) OrderStatus status,
                                       @RequestParam(required = false) PaymentStatus paymentStatus,
                                       @RequestParam(required = false) String userId,
                                       @RequestParam(required = false) String vendorId) {
        Pageable pageable = paginated ? PageRequest.of(page, size) : Pageable.unpaged();
        Page<OrderSummaryResponse> ordersPage = orderService.getOrders(pageable, status, paymentStatus, userId, vendorId);

        if (paginated) {
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(ordersPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(ordersPage.getTotalPages()))
                    .body(ordersPage.getContent());
        }

        List<OrderSummaryResponse> orders = ordersPage.getContent();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}/payment-schedule")
    public ResponseEntity<List<OrderPaymentScheduleResponse>> getOrderPaymentSchedule(@PathVariable String id) {
        List<OrderPaymentScheduleResponse> schedule = orderService.getOrderPaymentSchedule(id);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/payment-schedule")
    public ResponseEntity<?> getPaymentSchedules(@RequestParam(defaultValue = "false") boolean paginated,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) PaymentScheduleStatus status) {
        Pageable pageable = paginated ? PageRequest.of(page, size) : Pageable.unpaged();
        Page<OrderPaymentScheduleResponse> schedules = orderService.getPaymentSchedules(pageable, status);

        if (paginated) {
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(schedules.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(schedules.getTotalPages()))
                    .body(schedules.getContent());
        }

        return ResponseEntity.ok(schedules.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        OrderResponse response = orderService.createOrder(request, userIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable String id,
                                                     @Valid @RequestBody OrderRequest request) {
        return orderService.updateOrder(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        boolean deleted = orderService.deleteOrder(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable String id,
                                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderResponse response = orderService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/payment-terms")
    public ResponseEntity<OrderResponse> updatePaymentTerms(@PathVariable String id,
                                                            @Valid @RequestBody OrderPaymentTermsUpdateRequest request) {
        OrderResponse response = orderService.updatePaymentTerms(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/payment-schedule/{scheduleId}/payments")
    public ResponseEntity<OrderResponse> recordPayment(@PathVariable String orderId,
                                                       @PathVariable String scheduleId,
                                                       @Valid @RequestBody OrderPaymentCollectionRequest request) {
        OrderResponse response = orderService.recordPayment(orderId, scheduleId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}/payment-transactions")
    public ResponseEntity<java.util.List<OrderPaymentTransactionResponse>> getPaymentTransactions(
            @PathVariable String orderId) {
        java.util.List<OrderPaymentTransactionResponse> transactions = orderService.getPaymentTransactions(orderId);
        return ResponseEntity.ok(transactions);
    }

    // ==================== NEW PRODUCTION FLOW ENDPOINTS ====================

    /**
     * Confirm order - transition from PENDING to CONFIRMED
     * POST /api/orders/{id}/confirm
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable String id,
                                                      @RequestBody OrderConfirmRequest request) {
        request.setOrderId(id);
        OrderResponse response = orderService.confirmOrder(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Start production - transition from CONFIRMED to IN_PRODUCTION
     * POST /api/orders/{id}/start-production
     */
    @PostMapping("/{id}/start-production")
    public ResponseEntity<OrderResponse> startProduction(@PathVariable String id) {
        OrderResponse response = orderService.startProduction(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Ship order - transition from IN_PRODUCTION to SHIPPED
     * POST /api/orders/{id}/ship
     */
    @PostMapping("/{id}/ship")
    public ResponseEntity<OrderResponse> shipOrder(@PathVariable String id,
                                                   @RequestBody OrderShipRequest request) {
        request.setOrderId(id);
        OrderResponse response = orderService.shipOrder(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Complete order - transition from SHIPPED to COMPLETED
     * POST /api/orders/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<OrderResponse> completeOrder(@PathVariable String id,
                                                       @Valid @RequestBody OrderCompleteRequest request) {
        request.setOrderId(id);
        OrderResponse response = orderService.completeOrder(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get orders awaiting MISA SKU creation
     * These are CONFIRMED orders that need admin to create SKU in MISA before production can start
     * GET /api/orders/awaiting-misa-sku
     */
    @GetMapping("/awaiting-misa-sku")
    public ResponseEntity<List<OrderResponse>> getOrdersAwaitingMisaSku() {
        List<OrderResponse> orders = orderService.getOrdersAwaitingMisaSku();
        return ResponseEntity.ok(orders);
    }

    /**
     * Assign or update vendor for an order
     * Allowed statuses: NEW, CONFIRMED, IN_PRODUCTION
     */
    @PutMapping("/{id}/vendor")
    public ResponseEntity<OrderResponse> assignVendor(@PathVariable String id,
                                                      @Valid @RequestBody OrderVendorAssignRequest request) {
        OrderResponse response = orderService.assignVendor(id, request.getVendorId());
        return ResponseEntity.ok(response);
    }

    /**
     * Mark MISA SKU as created for an order
     * Must be called after admin creates the SKU in MISA
     * Required before production can start
     * POST /api/orders/{id}/misa-sku-created
     */
    @PostMapping("/{id}/misa-sku-created")
    public ResponseEntity<OrderResponse> markMisaSkuCreated(@PathVariable String id,
                                                            @RequestParam String misaItemId) {
        OrderResponse response = orderService.markMisaSkuCreated(id, misaItemId);
        return ResponseEntity.ok(response);
    }
}
