package com.mirror.product.service;

import com.mirror.product.entity.*;
import com.mirror.product.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service for Purchase Order management - Procurement system for raw materials
 * Migrated from mirror-mrp-service
 */
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderService.class);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final VendorRepository vendorRepository;
    private final MaterialInventoryRepository materialInventoryRepository;

    /**
     * Get all purchase orders
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrder> findAll() {
        return purchaseOrderRepository.findAll();
    }

    /**
     * Find purchase order by ID
     */
    @Transactional(readOnly = true)
    public Optional<PurchaseOrder> findById(UUID id) {
        return purchaseOrderRepository.findById(id);
    }

    /**
     * Find purchase orders by status
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrder> findByStatus(String status) {
        return purchaseOrderRepository.findByStatus(status);
    }

    /**
     * Find purchase orders by vendor ID
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrder> findByVendorId(String vendorId) {
        return purchaseOrderRepository.findByVendorId(vendorId);
    }

    /**
     * Find purchase orders by vendor ID and status
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrder> findByVendorIdAndStatus(String vendorId, String status) {
        return purchaseOrderRepository.findByVendorIdAndStatus(vendorId, status);
    }

    /**
     * Find purchase orders by expected delivery date range
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrder> findByExpectedDeliveryDateBetween(LocalDate startDate, LocalDate endDate) {
        return purchaseOrderRepository.findByExpectedDeliveryDateBetween(startDate, endDate);
    }

    /**
     * Find items by purchase order ID
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderItem> findItemsByPurchaseOrderId(UUID purchaseOrderId) {
        return purchaseOrderItemRepository.findByPurchaseOrderId(purchaseOrderId);
    }

    /**
     * Find items by material ID
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderItem> findItemsByMaterialId(UUID materialId) {
        return purchaseOrderItemRepository.findByMaterialId(materialId);
    }

    /**
     * Create new purchase order
     */
    @Transactional
    public PurchaseOrder create(PurchaseOrder purchaseOrder) {
        // Validate vendor exists
        if (purchaseOrder.getVendor() != null && purchaseOrder.getVendor().getId() != null) {
            Vendor vendor = vendorRepository.findById(purchaseOrder.getVendor().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + purchaseOrder.getVendor().getId()));
            purchaseOrder.setVendor(vendor);
        } else {
            throw new IllegalArgumentException("Vendor is required for purchase order");
        }

        // Set default status if not provided
        if (purchaseOrder.getStatus() == null || purchaseOrder.getStatus().isEmpty()) {
            purchaseOrder.setStatus("DRAFT");
        }

        // Process items
        if (purchaseOrder.getPoItems() != null && !purchaseOrder.getPoItems().isEmpty()) {
            // Set purchase order reference and calculate item prices
            for (PurchaseOrderItem item : purchaseOrder.getPoItems()) {
                item.setPurchaseOrder(purchaseOrder);

                // Validate and set material
                if (item.getMaterial() != null && item.getMaterial().getId() != null) {
                    MaterialInventory material = materialInventoryRepository.findById(item.getMaterial().getId())
                            .orElseThrow(() -> new IllegalArgumentException("Material not found with id: " + item.getMaterial().getId()));
                    item.setMaterial(material);
                }

                // Calculate item total price
                if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    item.setTotalPrice(itemTotal);
                }
            }

            // Calculate total cost after item prices are set
            BigDecimal totalCost = calculateTotalCost(purchaseOrder.getPoItems());
            purchaseOrder.setTotalCost(totalCost);
        }

        logger.info("Creating new purchase order for vendor: {} (status: {}, total cost: {})",
                purchaseOrder.getVendor().getId(), purchaseOrder.getStatus(), purchaseOrder.getTotalCost());

        return purchaseOrderRepository.save(purchaseOrder);
    }

    /**
     * Update existing purchase order
     */
    @Transactional
    public PurchaseOrder update(UUID id, PurchaseOrder purchaseOrderDetails) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found with id: " + id));

        // Update vendor if provided
        if (purchaseOrderDetails.getVendor() != null && purchaseOrderDetails.getVendor().getId() != null) {
            Vendor vendor = vendorRepository.findById(purchaseOrderDetails.getVendor().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + purchaseOrderDetails.getVendor().getId()));
            purchaseOrder.setVendor(vendor);
        }

        // Update status
        if (purchaseOrderDetails.getStatus() != null) {
            purchaseOrder.setStatus(purchaseOrderDetails.getStatus());
        }

        // Update expected delivery date
        if (purchaseOrderDetails.getExpectedDeliveryDate() != null) {
            purchaseOrder.setExpectedDeliveryDate(purchaseOrderDetails.getExpectedDeliveryDate());
        }

        // Update items if provided
        if (purchaseOrderDetails.getPoItems() != null) {
            // Remove old items
            if (purchaseOrder.getPoItems() != null) {
                purchaseOrder.getPoItems().clear();
            } else {
                purchaseOrder.setPoItems(new java.util.HashSet<>());
            }

            // Add new items
            for (PurchaseOrderItem item : purchaseOrderDetails.getPoItems()) {
                item.setPurchaseOrder(purchaseOrder);

                // Validate and set material
                if (item.getMaterial() != null && item.getMaterial().getId() != null) {
                    MaterialInventory material = materialInventoryRepository.findById(item.getMaterial().getId())
                            .orElseThrow(() -> new IllegalArgumentException("Material not found with id: " + item.getMaterial().getId()));
                    item.setMaterial(material);
                }

                // Calculate item total price
                if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    item.setTotalPrice(itemTotal);
                }

                purchaseOrder.getPoItems().add(item);
            }

            // Recalculate total cost
            BigDecimal totalCost = calculateTotalCost(purchaseOrder.getPoItems());
            purchaseOrder.setTotalCost(totalCost);
        }

        logger.info("Updating purchase order: {} (status: {}, total cost: {})",
                id, purchaseOrder.getStatus(), purchaseOrder.getTotalCost());

        return purchaseOrderRepository.save(purchaseOrder);
    }

    /**
     * Update purchase order status
     */
    @Transactional
    public PurchaseOrder updateStatus(UUID id, String status) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found with id: " + id));

        String oldStatus = purchaseOrder.getStatus();
        purchaseOrder.setStatus(status);

        logger.info("Updating purchase order {} status: {} -> {}", id, oldStatus, status);

        return purchaseOrderRepository.save(purchaseOrder);
    }

    /**
     * Delete purchase order by ID
     */
    @Transactional
    public void deleteById(UUID id) {
        if (!purchaseOrderRepository.existsById(id)) {
            throw new RuntimeException("Purchase order not found with id: " + id);
        }

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found with id: " + id));

        // Only allow deletion of DRAFT or CANCELLED orders
        if (!"DRAFT".equals(purchaseOrder.getStatus()) && !"CANCELLED".equals(purchaseOrder.getStatus())) {
            throw new IllegalStateException("Cannot delete purchase order with status: " + purchaseOrder.getStatus() +
                    ". Only DRAFT or CANCELLED orders can be deleted.");
        }

        logger.warn("Deleting purchase order: {} (vendor: {}, status: {})",
                id, purchaseOrder.getVendor().getId(), purchaseOrder.getStatus());

        purchaseOrderRepository.deleteById(id);
    }

    /**
     * Cancel purchase order
     */
    @Transactional
    public PurchaseOrder cancel(UUID id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found with id: " + id));

        if ("RECEIVED".equals(purchaseOrder.getStatus())) {
            throw new IllegalStateException("Cannot cancel purchase order that has already been received");
        }

        purchaseOrder.setStatus("CANCELLED");

        logger.info("Cancelled purchase order: {}", id);

        return purchaseOrderRepository.save(purchaseOrder);
    }

    /**
     * Count total purchase orders
     */
    @Transactional(readOnly = true)
    public long count() {
        return purchaseOrderRepository.count();
    }

    /**
     * Check if purchase order exists by ID
     */
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return purchaseOrderRepository.existsById(id);
    }

    /**
     * Calculate total cost from purchase order items
     */
    private BigDecimal calculateTotalCost(Set<PurchaseOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return items.stream()
                .filter(item -> item.getTotalPrice() != null)
                .map(PurchaseOrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
