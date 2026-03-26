package com.mirror.product.service;

import com.mirror.product.dto.OrderItemRequest;
import com.mirror.product.dto.OrderPaymentCollectionRequest;
import com.mirror.product.dto.OrderPaymentScheduleRequest;
import com.mirror.product.dto.OrderPaymentScheduleResponse;
import com.mirror.product.dto.OrderPaymentTransactionResponse;
import com.mirror.product.dto.OrderPaymentTermsUpdateRequest;
import com.mirror.product.dto.OrderRequest;
import com.mirror.product.dto.OrderResponse;
import com.mirror.product.dto.OrderStatusUpdateRequest;
import com.mirror.product.dto.OrderSummaryResponse;
import com.mirror.product.dto.OrderConfirmRequest;
import com.mirror.product.dto.OrderShipRequest;
import com.mirror.product.dto.OrderCompleteRequest;
import com.mirror.product.dto.OrderVendorAssignRequest;
import com.mirror.product.entity.Order;
import com.mirror.product.entity.OrderPaymentSchedule;
import com.mirror.product.entity.OrderPaymentTransaction;
import com.mirror.product.entity.OrderStatusHistory;
import com.mirror.product.enums.OrderStatus;
import com.mirror.product.enums.PaymentScheduleStatus;
import com.mirror.product.enums.PaymentStatus;
import com.mirror.product.enums.PaymentTermType;
import com.mirror.product.mapper.OrderMapper;
import com.mirror.product.repository.OrderPaymentScheduleRepository;
import com.mirror.product.repository.OrderPaymentTransactionRepository;
import com.mirror.product.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.RoundingMode;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderPaymentScheduleRepository orderPaymentScheduleRepository;
    private final OrderPaymentTransactionRepository orderPaymentTransactionRepository;
    private final OrderMapper orderMapper;
    private final MisaOrderIntegrationService misaOrderIntegrationService;

    /**
     * Create a new order along with items, payment schedule, and initial status history.
     */
    public OrderResponse createOrder(OrderRequest request, String userIdFromHeader) {
        Order order = orderMapper.toEntity(request);
        String effectiveUserId = (userIdFromHeader != null && !userIdFromHeader.isBlank())
                ? userIdFromHeader
                : request.getUserId();
        order.setUserId(effectiveUserId);
        order.setStatus(OrderStatus.NEW);
        ensureItemsPresent(order, request);
        ensurePaymentSchedule(order, request);
        attachInitialStatusHistory(order, order.getStatus(), request.getNotes(), effectiveUserId);
        recalculateFinancials(order);
        recalculatePaymentState(order);
        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    /**
     * Update order core fields, items, and payment schedule.
     */
    public Optional<OrderResponse> updateOrder(String orderId, OrderRequest request) {
        return orderRepository.findActiveById(orderId)
                .map(existing -> {
                    orderMapper.updateEntity(existing, request);
                    ensureItemsPresent(existing, request);
                    ensurePaymentSchedule(existing, request);
                    recalculateFinancials(existing);
                    recalculatePaymentState(existing);
                    Order saved = orderRepository.save(existing);
                    return orderMapper.toResponse(saved);
                });
    }

    /**
     * Soft delete order by marking it deleted and inactive.
     */
    public boolean deleteOrder(String orderId) {
        return orderRepository.findActiveById(orderId)
                .map(order -> {
                    order.setIsDeleted(true);
                    order.setIsActive(false);
                    orderRepository.save(order);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .filter(order -> Boolean.FALSE.equals(order.getIsDeleted()))
                .map(orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrders(Pageable pageable, OrderStatus status, PaymentStatus paymentStatus, String userId, String vendorId) {
        if (status != null) {
            return orderRepository.findByStatus(status, pageable).map(orderMapper::toSummaryResponse);
        }
        if (paymentStatus != null) {
            return orderRepository.findByPaymentStatus(paymentStatus, pageable).map(orderMapper::toSummaryResponse);
        }
        if (vendorId != null && !vendorId.isBlank()) {
            return orderRepository.findByVendorIdAndIsDeletedFalseOrderByCreatedAtDesc(vendorId, pageable)
                    .map(orderMapper::toSummaryResponse);
        }
        if (userId != null && !userId.isBlank()) {
            List<OrderSummaryResponse> summaries = orderMapper.toSummaryList(orderRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId));
            return new PageImpl<>(summaries, pageable, summaries.size());
        }
        return orderRepository.findAllActive(pageable).map(orderMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getOrdersByVendor(String vendorId) {
        List<Order> orders = orderRepository.findByVendorIdAndIsDeletedFalseOrderByCreatedAtDesc(vendorId);
        return orderMapper.toSummaryList(orders);
    }

    /**
     * Assign or update vendor on an order. Allowed statuses: NEW, CONFIRMED, IN_PRODUCTION.
     */
    public OrderResponse assignVendor(String orderId, String vendorId) {
        Order order = orderRepository.findActiveById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        Set<OrderStatus> allowed = Set.of(OrderStatus.NEW, OrderStatus.CONFIRMED, OrderStatus.IN_PRODUCTION);
        if (!allowed.contains(order.getStatus())) {
            throw new IllegalStateException("Cannot assign vendor when order status is " + order.getStatus());
        }

        if (vendorId == null || vendorId.isBlank()) {
            throw new IllegalArgumentException("vendorId is required");
        }

        order.setVendorId(vendorId);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(order.getStatus());
        history.setChangedAt(Instant.now());
        history.setNote("Vendor assigned: " + vendorId);
        order.addStatusHistory(history);

        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderPaymentScheduleResponse> getOrderPaymentSchedule(String orderId) {
        orderRepository.findById(orderId)
                .filter(order -> Boolean.FALSE.equals(order.getIsDeleted()))
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        List<OrderPaymentSchedule> schedules = orderPaymentScheduleRepository.findByOrder_IdOrderByDueDateAsc(orderId);
        return orderMapper.toPaymentScheduleResponseList(schedules);
    }

    @Transactional(readOnly = true)
    public Page<OrderPaymentScheduleResponse> getPaymentSchedules(Pageable pageable, PaymentScheduleStatus status) {
        Page<OrderPaymentSchedule> schedules;
        if (status != null) {
            schedules = orderPaymentScheduleRepository.findByStatus(status, pageable);
        } else {
            schedules = orderPaymentScheduleRepository.findAllActive(pageable);
        }
        return schedules.map(orderMapper::toPaymentScheduleResponse);
    }

    /**
     * Update the order status and record history.
     */
    public OrderResponse updateStatus(String orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findActiveById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (request.getStatus() != null && request.getStatus() != order.getStatus()) {
            order.setStatus(request.getStatus());
            order.setLastStatusUpdatedAt(Instant.now());
            attachInitialStatusHistory(order, request.getStatus(), request.getNote(), request.getChangedBy());
        }

        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    /**
     * Update order payment terms and optionally rebuild schedules.
     */
    public OrderResponse updatePaymentTerms(String orderId, OrderPaymentTermsUpdateRequest request) {
        Order order = orderRepository.findActiveById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        order.setPaymentTermsType(Optional.ofNullable(request.getPaymentTermsType()).orElse(PaymentTermType.FIXED_SCHEDULE));
        order.setPaymentTerms(request.getPaymentTerms());

        if (!CollectionUtils.isEmpty(request.getPaymentSchedule())) {
            validateScheduleAmountsSum(order.getTotalAmount(), request.getPaymentSchedule());
            order.clearPaymentSchedule();
            request.getPaymentSchedule().forEach(scheduleRequest -> order.addPaymentScheduleEntry(orderMapper.toPaymentSchedule(scheduleRequest)));
        }

        recalculatePaymentState(order);
        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    /**
     * Record a payment against a specific schedule entry.
     */
    public OrderResponse recordPayment(String orderId, String scheduleId, OrderPaymentCollectionRequest request) {
        Order order = orderRepository.findActiveById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        OrderPaymentSchedule schedule = orderPaymentScheduleRepository.findByIdAndOrder_Id(scheduleId, orderId)
                .orElseThrow(() -> new EntityNotFoundException("Payment schedule entry not found: " + scheduleId));

        BigDecimal newAmountPaid = schedule.getAmountPaid().add(request.getAmountPaid());
        schedule.setAmountPaid(newAmountPaid);
        if (request.getNote() != null && !request.getNote().isBlank()) {
            schedule.setNotes(request.getNote());
        }
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()) {
            schedule.setPaymentMethod(request.getPaymentMethod());
        }
        Instant paymentTime = Optional.ofNullable(request.getPaidAt()).orElseGet(Instant::now);
        schedule.setPaidAt(paymentTime);

        updateScheduleStatus(schedule);
        orderPaymentScheduleRepository.save(schedule);

        // Create payment transaction audit record
        OrderPaymentTransaction transaction = OrderPaymentTransaction.builder()
                .schedule(schedule)
                .order(order)
                .amount(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .paidAt(paymentTime)
                .recordedBy(request.getRecordedBy())
                .notes(request.getNote())
                .build();
        orderPaymentTransactionRepository.save(transaction);

        PaymentStatus previousPaymentStatus = order.getPaymentStatus();
        recalculatePaymentState(order);
        Order saved = orderRepository.save(order);

        // Trigger MISA invoice submission when payment becomes PAID
        if (saved.getPaymentStatus() == PaymentStatus.PAID
                && previousPaymentStatus != PaymentStatus.PAID
                && !Boolean.TRUE.equals(saved.getMisaSaleRecorded())) {
            misaOrderIntegrationService.submitSalesInvoice(saved.getId());
        }

        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderPaymentTransactionResponse> getPaymentTransactions(String orderId) {
        return orderPaymentTransactionRepository.findByOrderIdOrderByPaidAtDesc(orderId)
                .stream()
                .map(tx -> OrderPaymentTransactionResponse.builder()
                        .id(tx.getId())
                        .scheduleId(tx.getSchedule().getId())
                        .orderId(tx.getOrder().getId())
                        .amount(tx.getAmount())
                        .paymentMethod(tx.getPaymentMethod())
                        .paidAt(tx.getPaidAt())
                        .recordedBy(tx.getRecordedBy())
                        .notes(tx.getNotes())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .toList();
    }

    private void ensureItemsPresent(Order order, OrderRequest request) {
        if (!CollectionUtils.isEmpty(order.getItems())) {
            return;
        }
        BigDecimal total = Optional.ofNullable(request.getTotalAmount()).orElse(BigDecimal.ZERO);
        order.addItem(orderMapper.toOrderItem(defaultItemRequest(request.getProductId(), total, order.getQuantity())));
    }

    private void ensurePaymentSchedule(Order order, OrderRequest request) {
        if (!CollectionUtils.isEmpty(order.getPaymentSchedule())) {
            return;
        }
        BigDecimal total = Optional.ofNullable(request.getTotalAmount()).orElse(BigDecimal.ZERO);
        order.addPaymentScheduleEntry(orderMapper.toPaymentSchedule(defaultScheduleRequest(total)));
    }

    private OrderStatusHistory attachInitialStatusHistory(Order order, OrderStatus status, String note, String actor) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(status != null ? status : OrderStatus.PENDING)
                .note(note)
                .changedBy(actor)
                .changedAt(Instant.now())
                .build();
        order.addStatusHistory(history);
        return history;
    }

    private void validateScheduleAmountsSum(BigDecimal orderTotal, List<OrderPaymentScheduleRequest> schedules) {
        if (orderTotal == null || schedules == null || schedules.isEmpty()) {
            return;
        }
        BigDecimal scheduleSum = schedules.stream()
                .map(s -> Optional.ofNullable(s.getAmountDue()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (scheduleSum.setScale(2, RoundingMode.HALF_UP).compareTo(orderTotal.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalArgumentException(
                    "Payment schedule amounts sum (" + scheduleSum.setScale(2, RoundingMode.HALF_UP)
                    + ") does not match order total (" + orderTotal.setScale(2, RoundingMode.HALF_UP) + ")");
        }
    }

    private void enforceNoOverduePayments(Order order, String action) {
        boolean hasOverdue = order.getPaymentSchedule().stream()
                .anyMatch(s -> s.getStatus() == PaymentScheduleStatus.OVERDUE);
        if (hasOverdue) {
            throw new IllegalStateException("Cannot " + action + " order — there are overdue payment schedule entries. Please collect overdue payments first.");
        }
    }

    private void recalculateFinancials(Order order) {
        BigDecimal subtotal = order.getItems().stream()
                .map(item -> Optional.ofNullable(item.getTotalPrice()).orElse(item.getUnitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotalAmount(subtotal);
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            order.setTotalAmount(subtotal);
        }
    }

    private void recalculatePaymentState(Order order) {
        BigDecimal totalPaid = order.getPaymentSchedule().stream()
                .map(entry -> Optional.ofNullable(entry.getAmountPaid()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.getPaymentSchedule().forEach(this::updateScheduleStatus);

        BigDecimal outstanding = order.getTotalAmount().subtract(totalPaid);
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            outstanding = BigDecimal.ZERO;
        }
        order.setPaymentOutstanding(outstanding);

        PaymentStatus paymentStatus;
        if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
            paymentStatus = PaymentStatus.PAID;
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            // If any payment has been made but order not fully paid, show PARTIALLY_PAID
            // even if some schedules are overdue
            paymentStatus = PaymentStatus.PARTIALLY_PAID;
        } else if (isOrderOverdue(order)) {
            // Only show OVERDUE if nothing has been paid yet
            paymentStatus = PaymentStatus.OVERDUE;
        } else {
            paymentStatus = PaymentStatus.PENDING;
        }
        order.setPaymentStatus(paymentStatus);
    }

    private void updateScheduleStatus(OrderPaymentSchedule schedule) {
        BigDecimal due = Optional.ofNullable(schedule.getAmountDue()).orElse(BigDecimal.ZERO);
        BigDecimal paid = Optional.ofNullable(schedule.getAmountPaid()).orElse(BigDecimal.ZERO);

        if (paid.compareTo(due) >= 0) {
            schedule.setStatus(PaymentScheduleStatus.PAID);
            if (schedule.getPaidAt() == null) {
                schedule.setPaidAt(Instant.now());
            }
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            schedule.setStatus(PaymentScheduleStatus.PARTIALLY_PAID);
        } else if (schedule.getDueDate() != null && schedule.getDueDate().isBefore(Instant.now())) {
            schedule.setStatus(PaymentScheduleStatus.OVERDUE);
        } else {
            schedule.setStatus(PaymentScheduleStatus.PENDING);
        }
    }

    private boolean isOrderOverdue(Order order) {
        Instant now = Instant.now();
        return order.getPaymentSchedule().stream()
                .anyMatch(entry -> entry.getDueDate() != null
                        && entry.getDueDate().isBefore(now)
                        && entry.getAmountDue().compareTo(entry.getAmountPaid()) > 0);
    }

    private OrderItemRequest defaultItemRequest(String productId, BigDecimal totalAmount, Integer quantity) {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(productId);
        int safeQuantity = (quantity == null || quantity <= 0) ? 1 : quantity;
        BigDecimal unitPrice = (safeQuantity > 0)
                ? totalAmount.divide(BigDecimal.valueOf(safeQuantity), 2, RoundingMode.HALF_UP)
                : totalAmount;
        itemRequest.setQuantity(safeQuantity);
        itemRequest.setUnitPrice(unitPrice);
        itemRequest.setTotalPrice(totalAmount);
        return itemRequest;
    }

    private OrderPaymentScheduleRequest defaultScheduleRequest(BigDecimal totalAmount) {
        OrderPaymentScheduleRequest scheduleRequest = new OrderPaymentScheduleRequest();
        scheduleRequest.setDueDate(Instant.now());
        scheduleRequest.setAmountDue(totalAmount);
        scheduleRequest.setAmountPaid(BigDecimal.ZERO);
        return scheduleRequest;
    }

    // ==================== NEW PRODUCTION FLOW METHODS ====================

    /**
     * Confirm order - transition from PENDING to CONFIRMED
     */
    public OrderResponse confirmOrder(String orderId, OrderConfirmRequest request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.NEW) {
            throw new IllegalStateException("Only PENDING or NEW orders can be confirmed");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        order.setLastStatusUpdatedAt(Instant.now());

        if (request.getNotes() != null) {
            order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + request.getNotes() : request.getNotes());
        }

        // Add status history entry
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.CONFIRMED);
        history.setChangedAt(Instant.now());
        history.setNote("Order confirmed");
        order.addStatusHistory(history);

        Order savedOrder = orderRepository.save(order);

        // Trigger async MISA SKU creation
        misaOrderIntegrationService.submitSkuCreation(savedOrder.getId());

        return orderMapper.toResponse(savedOrder);
    }

    /**
     * Start production - transition from CONFIRMED to IN_PRODUCTION
     * REQUIRES: MISA SKU must be created first
     */
    public OrderResponse startProduction(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED orders can start production");
        }

        // ENFORCEMENT: Check if MISA SKU was created
        if (!Boolean.TRUE.equals(order.getMisaItemCreated())) {
            throw new IllegalStateException("Cannot start production: MISA SKU must be created first. Please create the SKU in MISA before starting production.");
        }

        order.setStatus(OrderStatus.IN_PRODUCTION);
        order.setProductionStartedAt(LocalDateTime.now());
        order.setLastStatusUpdatedAt(Instant.now());

        // Add status history entry
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.IN_PRODUCTION);
        history.setChangedAt(Instant.now());
        history.setNote("Production started");
        order.addStatusHistory(history);

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    /**
     * Ship order - transition from IN_PRODUCTION to SHIPPED
     * This will trigger MISA item creation (handled by caller/event)
     */
    public OrderResponse shipOrder(String orderId, OrderShipRequest request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.IN_PRODUCTION) {
            throw new IllegalStateException("Only IN_PRODUCTION orders can be shipped");
        }

        enforceNoOverduePayments(order, "ship");

        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        order.setLastStatusUpdatedAt(Instant.now());

        if (request.getNotes() != null) {
            order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + request.getNotes() : request.getNotes());
        }

        // Add status history entry with shipping details
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.SHIPPED);
        history.setChangedAt(Instant.now());
        String notes = "Order shipped";
        if (request.getTrackingNumber() != null) {
            notes += " - Tracking: " + request.getTrackingNumber();
        }
        if (request.getShippingCarrier() != null) {
            notes += " - Carrier: " + request.getShippingCarrier();
        }
        history.setNote(notes);
        order.addStatusHistory(history);

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    /**
     * Complete order - transition from SHIPPED to COMPLETED
     * This will trigger MISA sale recording (handled by caller/event)
     */
    public OrderResponse completeOrder(String orderId, OrderCompleteRequest request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Only SHIPPED orders can be completed");
        }

        enforceNoOverduePayments(order, "complete");

        if (!Boolean.TRUE.equals(request.getFinalPaymentReceived())) {
            throw new IllegalStateException("Final payment must be received before completing order");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setFinalPaymentReceived(true);
        order.setFinalPaymentDate(request.getFinalPaymentDate() != null ? request.getFinalPaymentDate() : LocalDateTime.now());
        order.setLastStatusUpdatedAt(Instant.now());

        if (request.getNotes() != null) {
            order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + request.getNotes() : request.getNotes());
        }

        // Add status history entry
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.COMPLETED);
        history.setChangedAt(Instant.now());
        String notes = "Order completed - Final payment received";
        if (request.getPaymentMethod() != null) {
            notes += " - Method: " + request.getPaymentMethod();
        }
        history.setNote(notes);
        order.addStatusHistory(history);

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    /**
     * Get orders by status
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);
        return orderMapper.toResponseList(orders);
    }

    /**
     * Get orders pending MISA item creation (SHIPPED but not created in MISA)
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersPendingMisaItemCreation() {
        List<Order> orders = orderRepository.findByStatusAndMisaItemCreatedOrderByShippedAtAsc(
            OrderStatus.SHIPPED, false);
        return orderMapper.toResponseList(orders);
    }

    /**
     * Get orders pending MISA sale recording (COMPLETED but not recorded in MISA)
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersPendingMisaSaleRecording() {
        List<Order> orders = orderRepository.findByStatusAndMisaSaleRecordedOrderByCompletedAtAsc(
            OrderStatus.COMPLETED, false);
        return orderMapper.toResponseList(orders);
    }

    /**
     * Get orders awaiting MISA SKU creation (CONFIRMED but MISA item not created yet)
     * These orders need admin action before production can start
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersAwaitingMisaSku() {
        List<Order> orders = orderRepository.findByStatusAndMisaItemCreatedOrderByConfirmedAtAsc(
            OrderStatus.CONFIRMED, false);
        return orderMapper.toResponseList(orders);
    }

    /**
     * Mark MISA SKU as created for an order
     * This must be called after admin creates the SKU in MISA
     * Required before production can start
     */
    public OrderResponse markMisaSkuCreated(String orderId, String misaItemId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Can only create MISA SKU for CONFIRMED orders");
        }

        if (misaItemId == null || misaItemId.isBlank()) {
            throw new IllegalArgumentException("MISA Item ID is required");
        }

        order.setMisaItemCreated(true);
        order.setMisaItemId(misaItemId);

        // Add status history entry
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.CONFIRMED);
        history.setChangedAt(Instant.now());
        history.setNote("MISA SKU created: " + misaItemId);
        order.addStatusHistory(history);

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }
} 
