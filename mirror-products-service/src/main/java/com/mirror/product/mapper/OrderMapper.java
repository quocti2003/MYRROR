package com.mirror.product.mapper;

import com.mirror.product.dto.ProductResponse;
import com.mirror.product.dto.VendorResponse;
import com.mirror.product.dto.OrderItemRequest;
import com.mirror.product.dto.OrderItemResponse;
import com.mirror.product.dto.OrderPaymentScheduleRequest;
import com.mirror.product.dto.OrderPaymentScheduleResponse;
import com.mirror.product.dto.OrderRequest;
import com.mirror.product.dto.OrderResponse;
import com.mirror.product.dto.OrderStatusHistoryResponse;
import com.mirror.product.dto.OrderSummaryResponse;
import com.mirror.product.entity.Order;
import com.mirror.product.entity.OrderItem;
import com.mirror.product.entity.OrderPaymentSchedule;
import com.mirror.product.entity.OrderStatusHistory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public Order toEntity(OrderRequest request) {
        if (request == null) {
            return null;
        }

        Order order = Order.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .vendorId(request.getVendorId())
                .subtotalAmount(defaultAmount(request.getSubtotalAmount()))
                .totalAmount(defaultAmount(request.getTotalAmount()))
                .currency(defaultCurrency(request.getCurrency()))
                .paymentTermsType(request.getPaymentTermsType())
                .paymentTerms(request.getPaymentTerms())
                .configuration(request.getConfiguration())
                .customerNotes(request.getCustomerNotes())
                .quantity(defaultQuantity(request.getQuantity()))
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .sourceChannel(request.getSourceChannel())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .notes(request.getNotes())
                .placedAt(Instant.now())
                .build();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            request.getItems().forEach(itemRequest -> order.addItem(toOrderItem(itemRequest)));
        }

        if (request.getPaymentSchedule() != null && !request.getPaymentSchedule().isEmpty()) {
            request.getPaymentSchedule().forEach(scheduleRequest -> order.addPaymentScheduleEntry(toPaymentSchedule(scheduleRequest)));
        }

        return order;
    }

    public void updateEntity(Order order, OrderRequest request) {
        if (order == null || request == null) {
            return;
        }

        order.setUserId(request.getUserId());
        order.setProductId(request.getProductId());
        order.setVendorId(request.getVendorId());
        order.setSubtotalAmount(defaultAmount(request.getSubtotalAmount()));
        order.setTotalAmount(defaultAmount(request.getTotalAmount()));
        order.setCurrency(defaultCurrency(request.getCurrency()));
        order.setPaymentTermsType(request.getPaymentTermsType());
        order.setPaymentTerms(request.getPaymentTerms());
        order.setConfiguration(request.getConfiguration());
        order.setCustomerNotes(request.getCustomerNotes());
        order.setQuantity(defaultQuantity(request.getQuantity()));
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setSourceChannel(request.getSourceChannel());
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setNotes(request.getNotes());

        order.clearItems();
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            request.getItems().forEach(itemRequest -> order.addItem(toOrderItem(itemRequest)));
        }

        order.clearPaymentSchedule();
        if (request.getPaymentSchedule() != null && !request.getPaymentSchedule().isEmpty()) {
            request.getPaymentSchedule().forEach(scheduleRequest -> order.addPaymentScheduleEntry(toPaymentSchedule(scheduleRequest)));
        }
    }

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setProductId(order.getProductId());
        response.setVendorId(order.getVendorId());
        if (order.getProduct() != null) {
            response.setProduct(new ProductResponse(order.getProduct()));
        }
        if (order.getVendor() != null) {
            response.setVendor(new VendorResponse(order.getVendor()));
        }
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentTermsType(order.getPaymentTermsType());
        response.setPaymentTerms(order.getPaymentTerms());
        response.setConfiguration(order.getConfiguration());
        response.setCustomerNotes(order.getCustomerNotes());
        response.setPaymentOutstanding(order.getPaymentOutstanding());
        response.setSubtotalAmount(order.getSubtotalAmount());
        response.setTotalAmount(order.getTotalAmount());
        response.setCurrency(order.getCurrency());
        response.setQuantity(order.getQuantity());
        response.setCustomerName(order.getCustomerName());
        response.setCustomerEmail(order.getCustomerEmail());
        response.setCustomerPhone(order.getCustomerPhone());
        response.setSourceChannel(order.getSourceChannel());
        response.setNotes(order.getNotes());
        response.setExpectedDeliveryDate(order.getExpectedDeliveryDate());
        response.setPlacedAt(order.getPlacedAt());
        response.setLastStatusUpdatedAt(order.getLastStatusUpdatedAt());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setOverdue(isOrderOverdue(order));

        if (order.getItems() != null) {
            response.setItems(order.getItems().stream()
                    .filter(Objects::nonNull)
                    .map(this::toItemResponse)
                    .collect(Collectors.toList()));
        }

        if (order.getPaymentSchedule() != null) {
            response.setPaymentSchedule(order.getPaymentSchedule().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(OrderPaymentSchedule::getDueDate))
                    .map(this::toPaymentScheduleResponse)
                    .collect(Collectors.toList()));
        }

        if (order.getStatusHistory() != null) {
            response.setStatusHistory(order.getStatusHistory().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(OrderStatusHistory::getChangedAt).reversed())
                    .map(this::toStatusHistoryResponse)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    public OrderSummaryResponse toSummaryResponse(Order order) {
        if (order == null) {
            return null;
        }

        OrderSummaryResponse response = new OrderSummaryResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setProductId(order.getProductId());
        response.setVendorId(order.getVendorId());
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setPaymentOutstanding(order.getPaymentOutstanding());
        response.setCurrency(order.getCurrency());
        response.setQuantity(order.getQuantity());
        response.setCustomerName(order.getCustomerName());
        response.setCustomerPhone(order.getCustomerPhone());
        response.setPlacedAt(order.getPlacedAt());
        response.setLastStatusUpdatedAt(order.getLastStatusUpdatedAt());
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }

    public List<OrderResponse> toResponseList(List<Order> orders) {
        if (orders == null) {
            return List.of();
        }
        return orders.stream()
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<OrderSummaryResponse> toSummaryList(List<Order> orders) {
        if (orders == null) {
            return List.of();
        }
        return orders.stream()
                .filter(Objects::nonNull)
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    public OrderItem toOrderItem(OrderItemRequest request) {
        if (request == null) {
            return null;
        }

        int quantity = Optional.ofNullable(request.getQuantity()).orElse(1);
        BigDecimal unitPrice = defaultAmount(request.getUnitPrice());
        BigDecimal totalPrice = Optional.ofNullable(request.getTotalPrice())
                .orElse(unitPrice.multiply(BigDecimal.valueOf(quantity)));

        return OrderItem.builder()
                .productId(request.getProductId())
                .productName(request.getProductName())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .metadata(request.getMetadata())
                .notes(request.getNotes())
                .build();
    }

    public OrderPaymentSchedule toPaymentSchedule(OrderPaymentScheduleRequest request) {
        if (request == null) {
            return null;
        }

        return OrderPaymentSchedule.builder()
                .dueDate(request.getDueDate())
                .amountDue(defaultAmount(request.getAmountDue()))
                .amountPaid(defaultAmount(request.getAmountPaid()))
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProductId());
        response.setProductName(item.getProductName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setTotalPrice(item.getTotalPrice());
        response.setMetadata(item.getMetadata());
        response.setNotes(item.getNotes());
        return response;
    }

    public List<OrderPaymentScheduleResponse> toPaymentScheduleResponseList(List<OrderPaymentSchedule> schedules) {
        if (schedules == null) {
            return List.of();
        }
        return schedules.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(OrderPaymentSchedule::getDueDate))
                .map(this::toPaymentScheduleResponse)
                .collect(Collectors.toList());
    }

    public OrderPaymentScheduleResponse toPaymentScheduleResponse(OrderPaymentSchedule schedule) {
        OrderPaymentScheduleResponse response = new OrderPaymentScheduleResponse();
        response.setId(schedule.getId());
        if (schedule.getOrder() != null) {
            response.setOrderId(schedule.getOrder().getId());
            response.setOrderStatus(schedule.getOrder().getStatus());
            response.setOrderPaymentStatus(schedule.getOrder().getPaymentStatus());
            response.setCurrency(schedule.getOrder().getCurrency());
            response.setOrderTotalAmount(schedule.getOrder().getTotalAmount());
            response.setCustomerName(schedule.getOrder().getCustomerName());
            response.setCustomerPhone(schedule.getOrder().getCustomerPhone());
        }
        response.setDueDate(schedule.getDueDate());
        response.setAmountDue(schedule.getAmountDue());
        response.setAmountPaid(schedule.getAmountPaid());
        BigDecimal outstanding = Optional.ofNullable(schedule.getAmountDue()).orElse(BigDecimal.ZERO)
                .subtract(Optional.ofNullable(schedule.getAmountPaid()).orElse(BigDecimal.ZERO));
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            outstanding = BigDecimal.ZERO;
        }
        response.setOutstanding(outstanding);
        response.setStatus(schedule.getStatus());
        response.setPaidAt(schedule.getPaidAt());
        response.setPaymentMethod(schedule.getPaymentMethod());
        response.setNotes(schedule.getNotes());
        response.setCreatedAt(schedule.getCreatedAt());
        return response;
    }

    private OrderStatusHistoryResponse toStatusHistoryResponse(OrderStatusHistory history) {
        OrderStatusHistoryResponse response = new OrderStatusHistoryResponse();
        response.setId(history.getId());
        response.setStatus(history.getStatus());
        response.setNote(history.getNote());
        response.setChangedBy(history.getChangedBy());
        response.setChangedAt(history.getChangedAt());
        return response;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String defaultCurrency(String currency) {
        return currency != null && !currency.isBlank() ? currency : "VND";
    }

    private Integer defaultQuantity(Integer quantity) {
        return (quantity == null || quantity <= 0) ? 1 : quantity;
    }

    private boolean isOrderOverdue(Order order) {
        if (order.getPaymentSchedule() == null || order.getPaymentSchedule().isEmpty()) {
            return false;
        }
        Instant now = Instant.now();
        return order.getPaymentSchedule().stream()
                .anyMatch(schedule -> schedule.getDueDate() != null
                        && schedule.getDueDate().isBefore(now)
                        && schedule.getAmountDue().compareTo(schedule.getAmountPaid()) > 0);
    }
}
