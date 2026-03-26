package com.mirror.product.service.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.entity.pod.PartnerInventory;
import com.mirror.product.entity.pod.PartnerSale;
import com.mirror.product.entity.pod.PartnerSaleItem;
import com.mirror.product.entity.pod.PodPartner;
import com.mirror.product.enums.PartnerSaleStatus;
import com.mirror.product.exception.pod.*;
import com.mirror.product.repository.pod.PartnerInventoryRepository;
import com.mirror.product.repository.pod.PartnerSaleItemRepository;
import com.mirror.product.repository.pod.PartnerSaleRepository;
import com.mirror.product.repository.pod.PodPartnerRepository;
import com.mirror.product.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class PartnerSaleService extends BaseService<PartnerSale, String> {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PartnerSaleRepository saleRepository;
    private final PartnerSaleItemRepository saleItemRepository;
    private final PartnerInventoryRepository inventoryRepository;
    private final PodPartnerRepository partnerRepository;
    private final PartnerInventoryService inventoryService;

    public PartnerSaleService(
            PartnerSaleRepository saleRepository,
            PartnerSaleItemRepository saleItemRepository,
            PartnerInventoryRepository inventoryRepository,
            PodPartnerRepository partnerRepository,
            PartnerInventoryService inventoryService
    ) {
        super(saleRepository);
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.partnerRepository = partnerRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional
    public PartnerSale update(String id, PartnerSale entity) {
        PartnerSale existing = saleRepository.findActiveById(id)
                .orElseThrow(() -> new PartnerSaleNotFoundException("Sale not found: " + id));
        existing.setNotes(entity.getNotes());
        return saleRepository.save(existing);
    }

    @Transactional
    public PartnerSaleResponse recordSale(String partnerId, PartnerSaleCreateRequest request) {
        log.info("Recording sale for partner: {}", partnerId);
        validatePhygitalPartner(partnerId);

        // Check for duplicate products
        Set<String> productIds = new HashSet<>();
        for (PartnerSaleItemRequest itemReq : request.getItems()) {
            if (!productIds.add(itemReq.getProductId())) {
                throw new InvalidSaleStateException("Duplicate product in sale items: " + itemReq.getProductId());
            }
        }

        PartnerSale sale = PartnerSale.builder()
                .partnerId(partnerId)
                .podId(request.getPodId())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .paymentMethod(request.getPaymentMethod())
                .paymentReference(request.getPaymentReference())
                .qrCodeId(request.getQrCodeId())
                .notes(request.getNotes())
                .items(new ArrayList<>())
                .build();

        sale = saleRepository.save(sale);

        // Add items and deduct inventory
        for (PartnerSaleItemRequest itemReq : request.getItems()) {
            PartnerInventory inventory = inventoryRepository
                    .findByPartnerIdAndProductIdAndIsDeletedFalse(partnerId, itemReq.getProductId())
                    .orElseThrow(() -> new InventoryNotFoundException("No inventory found for product: " + itemReq.getProductId()));

            if (inventory.getQuantityAvailable() < itemReq.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product " + itemReq.getProductId() +
                        ". Available: " + inventory.getQuantityAvailable());
            }

            PartnerSaleItem item = PartnerSaleItem.builder()
                    .saleId(sale.getId())
                    .productId(itemReq.getProductId())
                    .inventoryId(inventory.getId())
                    .quantity(itemReq.getQuantity())
                    .wholesaleCost(inventory.getWholesalePrice())
                    .sellingPrice(itemReq.getSellingPrice())
                    .build();
            item.calculateProfit();
            sale.getItems().add(item);

            // Deduct inventory
            inventoryService.deductForSale(sale.getId(), partnerId, itemReq.getProductId(), itemReq.getQuantity());
        }

        sale.calculateTotals();
        sale = saleRepository.save(sale);

        log.info("Sale recorded: {} with {} items", sale.getId(), sale.getItems().size());
        return PartnerSaleResponse.fromEntity(sale);
    }

    @Transactional
    public PartnerSaleResponse confirmSale(String partnerId, String saleId) {
        PartnerSale sale = getSaleEntityForPartner(saleId, partnerId);
        validateSaleStatusTransition(sale.getStatus(), PartnerSaleStatus.CONFIRMED);

        sale.setStatus(PartnerSaleStatus.CONFIRMED);
        sale = saleRepository.save(sale);

        log.info("Sale confirmed: {}", saleId);
        return PartnerSaleResponse.fromEntity(sale);
    }

    @Transactional
    public PartnerSaleResponse completeSale(String partnerId, String saleId) {
        PartnerSale sale = getSaleEntityForPartner(saleId, partnerId);
        validateSaleStatusTransition(sale.getStatus(), PartnerSaleStatus.COMPLETED);

        sale.setStatus(PartnerSaleStatus.COMPLETED);
        sale = saleRepository.save(sale);

        log.info("Sale completed: {}", saleId);
        return PartnerSaleResponse.fromEntity(sale);
    }

    @Transactional
    public PartnerSaleResponse cancelSale(String partnerId, String saleId, String reason) {
        PartnerSale sale = getSaleEntityForPartner(saleId, partnerId);
        if (sale.getStatus() == PartnerSaleStatus.COMPLETED || sale.getStatus() == PartnerSaleStatus.RETURNED) {
            throw new InvalidSaleStateException("Cannot cancel sale in status: " + sale.getStatus());
        }

        sale.setStatus(PartnerSaleStatus.CANCELLED);
        sale.setNotes((sale.getNotes() != null ? sale.getNotes() + "\n" : "") + "Cancelled: " + reason);
        sale = saleRepository.save(sale);

        // Restore inventory
        restoreInventoryForSale(sale);

        log.info("Sale cancelled: {}", saleId);
        return PartnerSaleResponse.fromEntity(sale);
    }

    @Transactional
    public PartnerSaleResponse returnSale(String partnerId, String saleId) {
        PartnerSale sale = getSaleEntityForPartner(saleId, partnerId);
        if (sale.getStatus() != PartnerSaleStatus.COMPLETED) {
            throw new InvalidSaleStateException("Can only return completed sales");
        }

        sale.setStatus(PartnerSaleStatus.RETURNED);
        sale = saleRepository.save(sale);

        // Restore inventory
        restoreInventoryForSale(sale);

        log.info("Sale returned: {}", saleId);
        return PartnerSaleResponse.fromEntity(sale);
    }

    @Transactional(readOnly = true)
    public PartnerSaleResponse getSale(String partnerId, String saleId) {
        return PartnerSaleResponse.fromEntity(getSaleEntityForPartner(saleId, partnerId));
    }

    @Transactional(readOnly = true)
    public Page<PartnerSaleResponse> getSalesByPartner(String partnerId, Pageable pageable) {
        return saleRepository.findByPartnerIdAndIsDeletedFalseOrderBySoldAtDesc(partnerId, pageable)
                .map(PartnerSaleResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public SalesStatistics getSalesStatistics(String partnerId, LocalDate startDate, LocalDate endDate) {
        Instant start = startDate.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();

        return SalesStatistics.builder()
                .totalSales(saleRepository.countSalesInPeriod(partnerId, start, end, PartnerSaleStatus.CANCELLED))
                .totalRevenue(saleRepository.calculateRevenueInPeriod(partnerId, start, end, PartnerSaleStatus.CANCELLED))
                .totalProfit(saleRepository.calculateProfitInPeriod(partnerId, start, end, PartnerSaleStatus.CANCELLED))
                .avgMargin(saleRepository.calculateAvgMarginInPeriod(partnerId, start, end, PartnerSaleStatus.CANCELLED))
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    @Transactional
    public int recalculateSaleProfits(String partnerId) {
        log.info("Recalculating sale profits for partner: {}", partnerId);
        List<PartnerSale> sales = saleRepository.findByPartnerIdAndIsDeletedFalse(partnerId);
        int updated = 0;
        for (PartnerSale sale : sales) {
            boolean changed = false;
            for (PartnerSaleItem item : sale.getItems()) {
                PartnerInventory inventory = inventoryRepository
                        .findByPartnerIdAndProductIdAndIsDeletedFalse(partnerId, item.getProductId())
                        .orElse(null);
                if (inventory != null && inventory.getWholesalePrice() != null
                        && item.getWholesaleCost().compareTo(inventory.getWholesalePrice()) != 0) {
                    log.info("Updating sale item {} wholesaleCost: {} -> {}",
                            item.getId(), item.getWholesaleCost(), inventory.getWholesalePrice());
                    item.setWholesaleCost(inventory.getWholesalePrice());
                    item.calculateProfit();
                    saleItemRepository.save(item);
                    changed = true;
                }
            }
            if (changed) {
                sale.calculateTotals();
                saleRepository.save(sale);
                updated++;
            }
        }
        log.info("Recalculated sale profits for partner {}: {} sales updated", partnerId, updated);
        return updated;
    }

    // === PRIVATE HELPERS ===

    private PartnerSale getSaleEntityForPartner(String saleId, String partnerId) {
        return saleRepository.findByIdAndPartnerIdAndIsDeletedFalse(saleId, partnerId)
                .orElseThrow(() -> new PartnerSaleNotFoundException("Sale not found: " + saleId));
    }

    private void validatePhygitalPartner(String partnerId) {
        PodPartner partner = partnerRepository.findActiveById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + partnerId));
        if (!partner.isPhygitalPartner()) {
            throw new UnauthorizedPartnerAccessException("Sales recording is only available for Phygital partners");
        }
    }

    private void validateSaleStatusTransition(PartnerSaleStatus current, PartnerSaleStatus target) {
        boolean valid = switch (target) {
            case CONFIRMED -> current == PartnerSaleStatus.PENDING;
            case COMPLETED -> current == PartnerSaleStatus.CONFIRMED;
            default -> false;
        };
        if (!valid) {
            throw new InvalidSaleStateException("Invalid sale status transition: " + current + " -> " + target);
        }
    }

    private void restoreInventoryForSale(PartnerSale sale) {
        if (sale.getItems() != null) {
            for (PartnerSaleItem item : sale.getItems()) {
                inventoryService.restoreForCancelledSale(
                        sale.getId(), sale.getPartnerId(), item.getProductId(), item.getQuantity());
            }
        }
    }

    // Inner class for sales statistics
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SalesStatistics {
        private long totalSales;
        private BigDecimal totalRevenue;
        private BigDecimal totalProfit;
        private BigDecimal avgMargin;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
