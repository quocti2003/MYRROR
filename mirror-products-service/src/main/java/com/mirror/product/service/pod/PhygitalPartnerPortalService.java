package com.mirror.product.service.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.entity.pod.PodPartner;
import com.mirror.product.enums.PartnerSaleStatus;
import com.mirror.product.enums.PartnerType;
import com.mirror.product.enums.WholesaleOrderStatus;
import com.mirror.product.exception.pod.PartnerNotFoundException;
import com.mirror.product.exception.pod.UnauthorizedPartnerAccessException;
import com.mirror.product.repository.pod.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PhygitalPartnerPortalService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PodPartnerRepository partnerRepository;
    private final PartnerInventoryRepository inventoryRepository;
    private final WholesaleOrderRepository wholesaleOrderRepository;
    private final PartnerSaleRepository saleRepository;
    private final PartnerInventoryService inventoryService;
    private final PartnerSaleService saleService;

    public PhygitalPartnerPortalService(
            PodPartnerRepository partnerRepository,
            PartnerInventoryRepository inventoryRepository,
            WholesaleOrderRepository wholesaleOrderRepository,
            PartnerSaleRepository saleRepository,
            PartnerInventoryService inventoryService,
            PartnerSaleService saleService
    ) {
        this.partnerRepository = partnerRepository;
        this.inventoryRepository = inventoryRepository;
        this.wholesaleOrderRepository = wholesaleOrderRepository;
        this.saleRepository = saleRepository;
        this.inventoryService = inventoryService;
        this.saleService = saleService;
    }

    @Transactional(readOnly = true)
    public PhygitalDashboardResponse getDashboard(String partnerId) {
        log.info("Building phygital dashboard for partner: {}", partnerId);
        validatePhygitalPartner(partnerId);

        LocalDate now = LocalDate.now(VIETNAM_ZONE);
        LocalDate thisMonthStart = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDate lastMonthEnd = thisMonthStart.minusDays(1);

        Instant thisMonthStartInstant = thisMonthStart.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant nowInstant = Instant.now();
        Instant lastMonthStartInstant = lastMonthStart.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant lastMonthEndInstant = lastMonthEnd.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();

        // Inventory stats
        long totalInventoryItems = inventoryRepository.countByPartner(partnerId);
        BigDecimal totalInventoryValue = inventoryRepository.calculateInventoryValue(partnerId);
        List<PartnerInventoryResponse> lowStockAlerts = inventoryService.getLowStockAlerts(partnerId);

        // Sales stats - this month
        long salesThisMonth = saleRepository.countSalesInPeriod(partnerId, thisMonthStartInstant, nowInstant, PartnerSaleStatus.CANCELLED);
        BigDecimal revenueThisMonth = saleRepository.calculateRevenueInPeriod(partnerId, thisMonthStartInstant, nowInstant, PartnerSaleStatus.CANCELLED);
        BigDecimal profitThisMonth = saleRepository.calculateProfitInPeriod(partnerId, thisMonthStartInstant, nowInstant, PartnerSaleStatus.CANCELLED);
        BigDecimal avgMarginThisMonth = saleRepository.calculateAvgMarginInPeriod(partnerId, thisMonthStartInstant, nowInstant, PartnerSaleStatus.CANCELLED);

        // Sales stats - last month
        long salesLastMonth = saleRepository.countSalesInPeriod(partnerId, lastMonthStartInstant, lastMonthEndInstant, PartnerSaleStatus.CANCELLED);
        BigDecimal revenueLastMonth = saleRepository.calculateRevenueInPeriod(partnerId, lastMonthStartInstant, lastMonthEndInstant, PartnerSaleStatus.CANCELLED);
        BigDecimal profitLastMonth = saleRepository.calculateProfitInPeriod(partnerId, lastMonthStartInstant, lastMonthEndInstant, PartnerSaleStatus.CANCELLED);

        // Growth
        BigDecimal revenueGrowth = calculateGrowth(revenueLastMonth, revenueThisMonth);
        BigDecimal profitGrowth = calculateGrowth(profitLastMonth, profitThisMonth);

        // Wholesale order stats
        long pendingOrders = wholesaleOrderRepository.countByPartnerAndStatus(partnerId, WholesaleOrderStatus.SUBMITTED)
                + wholesaleOrderRepository.countByPartnerAndStatus(partnerId, WholesaleOrderStatus.APPROVED);
        long inTransitOrders = wholesaleOrderRepository.countByPartnerAndStatus(partnerId, WholesaleOrderStatus.SHIPPED);
        BigDecimal totalPurchased = wholesaleOrderRepository.calculateTotalPurchased(partnerId, WholesaleOrderStatus.COMPLETED);

        // Recent sales
        List<PartnerSaleResponse> recentSales = saleRepository
                .findByPartnerIdAndIsDeletedFalseOrderBySoldAtDesc(partnerId, org.springframework.data.domain.PageRequest.of(0, 10))
                .map(PartnerSaleResponse::fromEntity)
                .getContent();

        return PhygitalDashboardResponse.builder()
                .totalInventoryItems(totalInventoryItems)
                .totalInventoryValue(totalInventoryValue)
                .lowStockCount(lowStockAlerts.size())
                .salesCountThisMonth(salesThisMonth)
                .revenueThisMonth(revenueThisMonth)
                .profitThisMonth(profitThisMonth)
                .avgMarginThisMonth(avgMarginThisMonth)
                .salesCountLastMonth(salesLastMonth)
                .revenueLastMonth(revenueLastMonth)
                .profitLastMonth(profitLastMonth)
                .revenueGrowthPercent(revenueGrowth)
                .profitGrowthPercent(profitGrowth)
                .pendingWholesaleOrders(pendingOrders)
                .inTransitWholesaleOrders(inTransitOrders)
                .totalPurchased(totalPurchased)
                .lowStockAlerts(lowStockAlerts)
                .recentSales(recentSales)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<PartnerResponse> getPhygitalPartners(Pageable pageable) {
        return partnerRepository.findByPartnerTypeAndIsDeletedFalse(PartnerType.PHYGITAL, pageable)
                .map(PartnerResponse::fromEntity);
    }

    // === PRIVATE HELPERS ===

    private void validatePhygitalPartner(String partnerId) {
        PodPartner partner = partnerRepository.findActiveById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + partnerId));
        if (!partner.isPhygitalPartner()) {
            throw new UnauthorizedPartnerAccessException("This dashboard is only available for Phygital partners");
        }
    }

    private BigDecimal calculateGrowth(BigDecimal previous, BigDecimal current) {
        if (current == null) {
            current = BigDecimal.ZERO;
        }
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0
                    ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }
}
