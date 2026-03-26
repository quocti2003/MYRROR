package com.mirror.product.service.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.pod.PodPartner;
import com.mirror.product.entity.pod.WholesaleOrder;
import com.mirror.product.entity.pod.WholesaleOrderItem;
import com.mirror.product.enums.WholesaleOrderStatus;
import com.mirror.product.exception.pod.*;
import com.mirror.product.repository.MirrorProductRepository;
import com.mirror.product.repository.pod.PodPartnerRepository;
import com.mirror.product.repository.pod.WholesaleOrderItemRepository;
import com.mirror.product.repository.pod.WholesaleOrderRepository;
import com.mirror.product.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WholesaleOrderService extends BaseService<WholesaleOrder, String> {

    private final WholesaleOrderRepository orderRepository;
    private final WholesaleOrderItemRepository itemRepository;
    private final PodPartnerRepository partnerRepository;
    private final MirrorProductRepository productRepository;
    private final PartnerInventoryService inventoryService;

    public WholesaleOrderService(
            WholesaleOrderRepository orderRepository,
            WholesaleOrderItemRepository itemRepository,
            PodPartnerRepository partnerRepository,
            MirrorProductRepository productRepository,
            PartnerInventoryService inventoryService
    ) {
        super(orderRepository);
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.partnerRepository = partnerRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional
    public WholesaleOrder update(String id, WholesaleOrder entity) {
        WholesaleOrder existing = orderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new WholesaleOrderNotFoundException("Wholesale order not found: " + id));
        existing.setShippingAddress(entity.getShippingAddress());
        existing.setNotes(entity.getNotes());
        return orderRepository.save(existing);
    }

    @Transactional
    public WholesaleOrderResponse createOrder(String partnerId, WholesaleOrderCreateRequest request) {
        log.info("Creating wholesale order for partner: {}", partnerId);
        PodPartner partner = validatePhygitalPartner(partnerId);

        WholesaleOrder order = WholesaleOrder.builder()
                .partnerId(partnerId)
                .shippingAddress(request.getShippingAddress())
                .shippingMethod(request.getShippingMethod())
                .notes(request.getNotes())
                .items(new ArrayList<>())
                .build();

        order = orderRepository.save(order);

        // Add items
        for (WholesaleOrderItemRequest itemReq : request.getItems()) {
            MirrorProduct product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new InventoryNotFoundException("Product not found: " + itemReq.getProductId()));

            // Validate stock availability
            int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (itemReq.getQuantity() > availableStock) {
                throw new InsufficientStockException(
                        "Insufficient stock for product \"" + product.getItemName() + "\". " +
                        "Requested: " + itemReq.getQuantity() + ", Available: " + availableStock);
            }

            BigDecimal retailPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal wholesalePrice = partner.getWholesalePrice(retailPrice);

            WholesaleOrderItem item = WholesaleOrderItem.builder()
                    .orderId(order.getId())
                    .productId(itemReq.getProductId())
                    .quantity(itemReq.getQuantity())
                    .retailPrice(retailPrice)
                    .wholesalePrice(wholesalePrice)
                    .build();
            item.calculateLineTotal();
            order.getItems().add(item);
        }

        order.calculateTotals();
        order = orderRepository.save(order);

        log.info("Wholesale order created: {}", order.getId());
        return WholesaleOrderResponse.fromEntity(order);
    }

    @Transactional
    public WholesaleOrderResponse submitOrder(String partnerId, String orderId) {
        WholesaleOrder order = getOrderEntityForPartner(orderId, partnerId);
        validateStatusTransition(order.getStatus(), WholesaleOrderStatus.SUBMITTED);

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new InvalidOrderStateException("Cannot submit an order with no items");
        }

        order.submit();
        order = orderRepository.save(order);

        log.info("Wholesale order submitted: {}", orderId);
        return WholesaleOrderResponse.fromEntity(order);
    }

    @Transactional
    public WholesaleOrderResponse approveOrder(String orderId, String approver) {
        WholesaleOrder order = getOrderEntity(orderId);
        validateStatusTransition(order.getStatus(), WholesaleOrderStatus.APPROVED);

        // Re-validate stock availability before approving
        for (WholesaleOrderItem item : order.getItems()) {
            MirrorProduct product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new InventoryNotFoundException("Product not found: " + item.getProductId()));
            int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (item.getQuantity() > availableStock) {
                throw new InsufficientStockException(
                        "Insufficient stock for product \"" + product.getItemName() + "\". " +
                        "Requested: " + item.getQuantity() + ", Available: " + availableStock);
            }
        }

        order.approve(approver);
        order = orderRepository.save(order);

        log.info("Wholesale order approved: {} by {}", orderId, approver);
        return WholesaleOrderResponse.fromEntity(order);
    }

    @Transactional
    public WholesaleOrderResponse processOrder(String orderId) {
        WholesaleOrder order = getOrderEntity(orderId);
        validateStatusTransition(order.getStatus(), WholesaleOrderStatus.PROCESSING);

        order.setStatus(WholesaleOrderStatus.PROCESSING);
        order = orderRepository.save(order);

        log.info("Wholesale order processing: {}", orderId);
        return WholesaleOrderResponse.fromEntity(order);
    }

    @Transactional
    public WholesaleOrderResponse shipOrder(String orderId, String trackingNumber) {
        WholesaleOrder order = getOrderEntity(orderId);
        validateStatusTransition(order.getStatus(), WholesaleOrderStatus.SHIPPED);

        // Deduct stock from Mirror's warehouse when items are shipped out
        for (WholesaleOrderItem item : order.getItems()) {
            MirrorProduct product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new InventoryNotFoundException("Product not found: " + item.getProductId()));
            int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (item.getQuantity() > currentStock) {
                throw new InsufficientStockException(
                        "Insufficient stock for product \"" + product.getItemName() + "\". " +
                        "Requested: " + item.getQuantity() + ", Available: " + currentStock);
            }
            product.setStockQuantity(currentStock - item.getQuantity());
            productRepository.save(product);
            log.info("Stock deducted for product {}: {} -> {}", product.getId(),
                    currentStock, product.getStockQuantity());
        }

        order.setStatus(WholesaleOrderStatus.SHIPPED);
        order.setTrackingNumber(trackingNumber);
        order.setShippedAt(Instant.now());
        order = orderRepository.save(order);

        log.info("Wholesale order shipped: {}, tracking: {}", orderId, trackingNumber);
        return WholesaleOrderResponse.fromEntity(order);
    }

    @Transactional
    public WholesaleOrderResponse confirmDelivery(String partnerId, String orderId) {
        WholesaleOrder order = getOrderEntityForPartner(orderId, partnerId);
        validateStatusTransition(order.getStatus(), WholesaleOrderStatus.DELIVERED);

        order.setStatus(WholesaleOrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());
        order = orderRepository.save(order);

        // Receive inventory at delivery
        var itemResponses = order.getItems().stream()
                .map(WholesaleOrderItemResponse::fromEntity)
                .collect(Collectors.toList());
        inventoryService.receiveWholesaleOrder(orderId, order.getPartnerId(), itemResponses);

        log.info("Wholesale order delivered: {}", orderId);
        return WholesaleOrderResponse.fromEntity(order);
    }

    @Transactional
    public WholesaleOrderResponse completeOrder(String orderId) {
        WholesaleOrder order = getOrderEntity(orderId);
        validateStatusTransition(order.getStatus(), WholesaleOrderStatus.COMPLETED);

        order.setStatus(WholesaleOrderStatus.COMPLETED);
        order.setCompletedAt(Instant.now());
        order = orderRepository.save(order);

        log.info("Wholesale order completed: {}", orderId);
        return WholesaleOrderResponse.fromEntity(order);
    }

    @Transactional
    public WholesaleOrderResponse cancelOrder(String partnerId, String orderId, String reason, String cancelledBy) {
        WholesaleOrder order = getOrderEntityForPartner(orderId, partnerId);
        if (order.getStatus() == WholesaleOrderStatus.SHIPPED ||
            order.getStatus() == WholesaleOrderStatus.DELIVERED ||
            order.getStatus() == WholesaleOrderStatus.COMPLETED) {
            throw new InvalidOrderStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.cancel(cancelledBy, reason);
        order = orderRepository.save(order);

        log.info("Wholesale order cancelled: {}", orderId);
        return WholesaleOrderResponse.fromEntity(order);
    }

    @Transactional
    public WholesaleOrderResponse adminCancelOrder(String orderId, String reason, String cancelledBy) {
        WholesaleOrder order = getOrderEntity(orderId);
        if (order.getStatus() == WholesaleOrderStatus.SHIPPED ||
            order.getStatus() == WholesaleOrderStatus.DELIVERED ||
            order.getStatus() == WholesaleOrderStatus.COMPLETED) {
            throw new InvalidOrderStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.cancel(cancelledBy, reason);
        order = orderRepository.save(order);

        log.info("Wholesale order cancelled by admin: {}", orderId);
        return WholesaleOrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public WholesaleOrderResponse getOrder(String partnerId, String orderId) {
        WholesaleOrder order = getOrderEntityForPartner(orderId, partnerId);
        return WholesaleOrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public WholesaleOrderResponse getOrderAdmin(String orderId) {
        WholesaleOrder order = getOrderEntity(orderId);
        return WholesaleOrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public Page<WholesaleOrderResponse> getOrdersByPartner(String partnerId, Pageable pageable) {
        return orderRepository.findByPartnerIdAndIsDeletedFalseOrderByCreatedAtDesc(partnerId, pageable)
                .map(WholesaleOrderResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WholesaleOrderResponse> searchOrders(WholesaleOrderStatus status, Pageable pageable) {
        if (status != null) {
            return orderRepository.findByStatusAndIsDeletedFalse(status, pageable)
                    .map(WholesaleOrderResponse::fromEntity);
        }
        return orderRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable)
                .map(WholesaleOrderResponse::fromEntity);
    }

    // === PRIVATE HELPERS ===

    private WholesaleOrder getOrderEntity(String orderId) {
        return orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new WholesaleOrderNotFoundException("Wholesale order not found: " + orderId));
    }

    private WholesaleOrder getOrderEntityForPartner(String orderId, String partnerId) {
        return orderRepository.findByIdAndPartnerIdAndIsDeletedFalse(orderId, partnerId)
                .orElseThrow(() -> new WholesaleOrderNotFoundException("Wholesale order not found: " + orderId));
    }

    private PodPartner validatePhygitalPartner(String partnerId) {
        PodPartner partner = partnerRepository.findActiveById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + partnerId));
        if (!partner.isPhygitalPartner()) {
            throw new UnauthorizedPartnerAccessException("Wholesale orders are only available for Phygital partners");
        }
        return partner;
    }

    private void validateStatusTransition(WholesaleOrderStatus current, WholesaleOrderStatus target) {
        boolean valid = switch (target) {
            case SUBMITTED -> current == WholesaleOrderStatus.DRAFT;
            case APPROVED -> current == WholesaleOrderStatus.SUBMITTED;
            case PROCESSING -> current == WholesaleOrderStatus.APPROVED;
            case SHIPPED -> current == WholesaleOrderStatus.PROCESSING;
            case DELIVERED -> current == WholesaleOrderStatus.SHIPPED;
            case COMPLETED -> current == WholesaleOrderStatus.DELIVERED;
            default -> false;
        };
        if (!valid) {
            throw new InvalidOrderStateException("Invalid status transition: " + current + " -> " + target);
        }
    }
}
