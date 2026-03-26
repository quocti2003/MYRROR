package com.mirror.product.service;

import com.mirror.product.dto.sourcing.*;
import com.mirror.product.entity.*;
import com.mirror.product.enums.ProductionPlanStatus;
import com.mirror.product.exception.BadRequestException;
import com.mirror.product.exception.ResourceNotFoundException;
import com.mirror.product.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating sourcing reports
 *
 * Sourcing reports aggregate production order data by assigned vendor,
 * including JTRC specifications and material summaries to help partners
 * understand what they need to source/produce.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SourcingReportService {

    private final ProductionPlanRepository productionPlanRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final ProductionOrderStageRepository productionOrderStageRepository;
    private final VendorRepository vendorRepository;

    /**
     * Generate sourcing reports for all vendors assigned to a production plan
     */
    @Transactional(readOnly = true)
    public List<SourcingReportResponse> generateReportsForPlan(String planId, String generatedBy) {
        log.info("Generating sourcing reports for plan: {}", planId);

        ProductionPlan plan = productionPlanRepository.findActiveById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionPlan", planId));

        // Validate plan status - should be at least PLANNING
        if (plan.getStatus() == ProductionPlanStatus.DRAFT) {
            throw new BadRequestException("Cannot generate sourcing reports for a DRAFT plan. Move to PLANNING first.");
        }

        // Get all distinct vendors assigned to stages in this plan
        List<String> vendorIds = productionOrderStageRepository.findDistinctVendorIdsByPlanId(planId);

        if (vendorIds.isEmpty()) {
            log.info("No vendors assigned to stages in plan: {}", planId);
            return Collections.emptyList();
        }

        // Generate report for each vendor
        List<SourcingReportResponse> reports = new ArrayList<>();
        for (String vendorId : vendorIds) {
            SourcingReportResponse report = generateReportForVendor(planId, vendorId, generatedBy);
            reports.add(report);
        }

        log.info("Generated {} sourcing reports for plan: {}", reports.size(), planId);
        return reports;
    }

    /**
     * Generate sourcing reports for all vendors assigned to a production plan (paginated)
     */
    @Transactional(readOnly = true)
    public Page<SourcingReportResponse> generateReportsForPlanPaginated(String planId, String generatedBy, Pageable pageable) {
        log.info("Generating paginated sourcing reports for plan: {} (page={}, size={})",
                planId, pageable.getPageNumber(), pageable.getPageSize());

        ProductionPlan plan = productionPlanRepository.findActiveById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionPlan", planId));

        // Validate plan status - should be at least PLANNING
        if (plan.getStatus() == ProductionPlanStatus.DRAFT) {
            throw new BadRequestException("Cannot generate sourcing reports for a DRAFT plan. Move to PLANNING first.");
        }

        // Get all distinct vendors assigned to stages in this plan
        List<String> allVendorIds = productionOrderStageRepository.findDistinctVendorIdsByPlanId(planId);

        if (allVendorIds.isEmpty()) {
            log.info("No vendors assigned to stages in plan: {}", planId);
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Apply pagination to vendor IDs
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allVendorIds.size());

        if (start >= allVendorIds.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, allVendorIds.size());
        }

        List<String> paginatedVendorIds = allVendorIds.subList(start, end);

        // Generate report for each vendor in the page
        List<SourcingReportResponse> reports = new ArrayList<>();
        for (String vendorId : paginatedVendorIds) {
            SourcingReportResponse report = generateReportForVendor(planId, vendorId, generatedBy);
            reports.add(report);
        }

        log.info("Generated {} sourcing reports (page {} of {}) for plan: {}",
                reports.size(), pageable.getPageNumber(),
                (int) Math.ceil((double) allVendorIds.size() / pageable.getPageSize()), planId);

        return new PageImpl<>(reports, pageable, allVendorIds.size());
    }

    /**
     * Generate sourcing report for a specific vendor within a production plan
     */
    @Transactional(readOnly = true)
    public SourcingReportResponse generateReportForVendor(String planId, String vendorId, String generatedBy) {
        log.info("Generating sourcing report for plan: {}, vendor: {}", planId, vendorId);

        ProductionPlan plan = productionPlanRepository.findActiveById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionPlan", planId));

        Vendor vendor = vendorRepository.findActiveById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId));

        // Get all stages assigned to this vendor for this plan
        List<ProductionOrderStage> stages = productionOrderStageRepository
                .findStagesByPlanAndVendor(planId, vendorId);

        // Build order items from stages
        List<SourcingOrderItem> orderItems = new ArrayList<>();
        BigDecimal totalEstimatedCost = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (ProductionOrderStage stage : stages) {
            ProductionOrder order = stage.getProductionOrder();
            SourcingOrderItem item = buildOrderItem(stage, order);
            orderItems.add(item);

            totalQuantity += order.getQuantity() != null ? order.getQuantity() : 1;
            if (stage.getEstimatedCost() != null) {
                totalEstimatedCost = totalEstimatedCost.add(stage.getEstimatedCost());
            }
        }

        // Build material aggregation summary
        MaterialAggregationSummary materialSummary = buildMaterialAggregation(stages);

        // Build report response
        return SourcingReportResponse.builder()
                .reportId(UUID.randomUUID().toString())
                .productionPlanId(planId)
                .productionPlanName(plan.getName())
                .vendorId(vendorId)
                .vendorName(vendor.getName())
                .vendorCode(vendor.getCode())
                .vendorEmail(vendor.getContactEmail())
                .vendorPhone(vendor.getContactPhone())
                .vendorCountry(vendor.getCountry())
                .generatedAt(LocalDateTime.now())
                .generatedBy(generatedBy)
                .targetStartDate(plan.getTargetStartDate())
                .targetEndDate(plan.getTargetEndDate())
                .orders(orderItems)
                .materialSummary(materialSummary)
                .totalOrders(orderItems.size())
                .totalQuantity(totalQuantity)
                .estimatedTotalCost(totalEstimatedCost)
                .sendStatus("DRAFT")
                .build();
    }

    /**
     * Build a sourcing order item from a stage and its order
     */
    private SourcingOrderItem buildOrderItem(ProductionOrderStage stage, ProductionOrder order) {
        SourcingOrderItem.SourcingOrderItemBuilder builder = SourcingOrderItem.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .quantity(order.getQuantity())
                .status(order.getStatus().name())
                .stageId(stage.getId())
                .stageName(stage.getStageName())
                .stageOrder(stage.getStageOrder())
                .requiredCapability(stage.getRequiredCapability() != null ? stage.getRequiredCapability().name() : null)
                .plannedStartDate(stage.getPlannedStartDate())
                .plannedEndDate(stage.getPlannedEndDate())
                .estimatedCost(stage.getEstimatedCost());

        // Add JTRC specifications if linked
        JewelryTechnicalReport jtrc = order.getJtrc();
        if (jtrc != null) {
            builder.jtrcId(jtrc.getId())
                    .reportNumber(jtrc.getReportNumber())
                    .collection(jtrc.getCollection())
                    .season(jtrc.getSeason())
                    .category(jtrc.getCategory())
                    .render3dUrl(jtrc.getRender3dUrl())
                    .stoneMapUrl(jtrc.getStoneMapUrl())
                    .technicalDrawingUrl(jtrc.getTechnicalDrawingUrl())
                    .productionNotes(jtrc.getProductionNotes())
                    .productionDifficulty(jtrc.getProductionDifficulty())
                    .estimatedLeadTimeDays(jtrc.getEstimatedLeadTimeDays());

            // Metal spec
            if (jtrc.getMetalComponent() != null) {
                builder.metalSpec(buildMetalSpec(jtrc.getMetalComponent()));
            }

            // Stone specs
            if (jtrc.getStoneComponents() != null && !jtrc.getStoneComponents().isEmpty()) {
                builder.stoneSpecs(jtrc.getStoneComponents().stream()
                        .map(this::buildStoneSpec)
                        .collect(Collectors.toList()));
            }

            // Labor specs
            if (jtrc.getLaborComponents() != null && !jtrc.getLaborComponents().isEmpty()) {
                builder.laborSpecs(jtrc.getLaborComponents().stream()
                        .map(this::buildLaborSpec)
                        .collect(Collectors.toList()));
            }
        }

        return builder.build();
    }

    /**
     * Build metal specification DTO
     */
    private SourcingMetalSpec buildMetalSpec(JTRCMetalComponent metal) {
        BigDecimal lossRate = metal.getLossRatePercent() != null
                ? metal.getLossRatePercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalWeight = metal.getWeightGrams();
        if (totalWeight != null && lossRate.compareTo(BigDecimal.ZERO) > 0) {
            totalWeight = totalWeight.multiply(BigDecimal.ONE.add(lossRate))
                    .setScale(3, RoundingMode.HALF_UP);
        }

        return SourcingMetalSpec.builder()
                .metalType(metal.getMetalType())
                .purity(metal.getMetalPurity())
                .grossWeight(metal.getWeightGrams())
                .netWeight(metal.getWeightGrams())
                .lossRate(metal.getLossRatePercent())
                .totalWeight(totalWeight)
                .estimatedCost(metal.getTotalCost())
                .build();
    }

    /**
     * Build stone specification DTO
     */
    private SourcingStoneSpec buildStoneSpec(JTRCStoneComponent stone) {
        BigDecimal totalCaratWeight = stone.getWeightCarat();
        if (totalCaratWeight != null && stone.getQuantity() != null) {
            totalCaratWeight = totalCaratWeight.multiply(BigDecimal.valueOf(stone.getQuantity()));
        }

        return SourcingStoneSpec.builder()
                .stoneRole(stone.getStoneRole() != null ? stone.getStoneRole().name() : null)
                .stoneType(stone.getStoneType())
                .shape(stone.getShape())
                .colorCategory(stone.getColorCategory() != null ? stone.getColorCategory().name() : null)
                .colorGrade(stone.getColorGrade())
                .clarityGrade(stone.getClarity())
                .caratWeight(stone.getWeightCarat())
                .quantity(stone.getQuantity())
                .totalCaratWeight(totalCaratWeight)
                .sizeRange(stone.getSizeMm())
                .pricePerCarat(stone.getUnitPrice())
                .estimatedCost(stone.getTotalPrice())
                .build();
    }

    /**
     * Build labor specification DTO
     */
    private SourcingLaborSpec buildLaborSpec(JTRCLaborComponent labor) {
        return SourcingLaborSpec.builder()
                .laborType(labor.getLaborType() != null ? labor.getLaborType().name() : null)
                .description(labor.getDescription())
                .unitCost(labor.getCost())
                .quantity(1)
                .totalCost(labor.getCost())
                .build();
    }

    /**
     * Build material aggregation summary from stages
     */
    private MaterialAggregationSummary buildMaterialAggregation(List<ProductionOrderStage> stages) {
        // Maps for aggregation
        Map<String, MaterialAggregationSummary.MetalAggregation> metalMap = new HashMap<>();
        Map<String, MaterialAggregationSummary.StoneAggregation> stoneMap = new HashMap<>();

        BigDecimal totalMetalCost = BigDecimal.ZERO;
        BigDecimal totalStoneCost = BigDecimal.ZERO;
        BigDecimal totalLaborCost = BigDecimal.ZERO;

        for (ProductionOrderStage stage : stages) {
            ProductionOrder order = stage.getProductionOrder();
            JewelryTechnicalReport jtrc = order.getJtrc();

            if (jtrc == null) continue;

            int qty = order.getQuantity() != null ? order.getQuantity() : 1;

            // Aggregate metals
            if (jtrc.getMetalComponent() != null) {
                JTRCMetalComponent metal = jtrc.getMetalComponent();
                String metalKey = metal.getMetalType() + "|" + metal.getMetalPurity();

                metalMap.compute(metalKey, (k, existing) -> {
                    BigDecimal weight = metal.getWeightGrams() != null
                            ? metal.getWeightGrams().multiply(BigDecimal.valueOf(qty))
                            : BigDecimal.ZERO;
                    BigDecimal cost = metal.getTotalCost() != null
                            ? metal.getTotalCost().multiply(BigDecimal.valueOf(qty))
                            : BigDecimal.ZERO;

                    if (existing == null) {
                        return MaterialAggregationSummary.MetalAggregation.builder()
                                .metalType(metal.getMetalType())
                                .purity(metal.getMetalPurity())
                                .totalWeight(weight)
                                .orderCount(1)
                                .estimatedCost(cost)
                                .build();
                    } else {
                        existing.setTotalWeight(existing.getTotalWeight().add(weight));
                        existing.setOrderCount(existing.getOrderCount() + 1);
                        existing.setEstimatedCost(existing.getEstimatedCost().add(cost));
                        return existing;
                    }
                });

                if (metal.getTotalCost() != null) {
                    totalMetalCost = totalMetalCost.add(metal.getTotalCost().multiply(BigDecimal.valueOf(qty)));
                }
            }

            // Aggregate stones
            if (jtrc.getStoneComponents() != null) {
                for (JTRCStoneComponent stone : jtrc.getStoneComponents()) {
                    String stoneKey = stone.getStoneType() + "|" + stone.getShape() + "|" + stone.getSizeMm();

                    stoneMap.compute(stoneKey, (k, existing) -> {
                        int stoneQty = stone.getQuantity() != null ? stone.getQuantity() * qty : qty;
                        BigDecimal caratWeight = stone.getWeightCarat() != null
                                ? stone.getWeightCarat().multiply(BigDecimal.valueOf(stoneQty))
                                : BigDecimal.ZERO;
                        BigDecimal cost = stone.getTotalPrice() != null
                                ? stone.getTotalPrice().multiply(BigDecimal.valueOf(qty))
                                : BigDecimal.ZERO;

                        if (existing == null) {
                            return MaterialAggregationSummary.StoneAggregation.builder()
                                    .stoneType(stone.getStoneType())
                                    .shape(stone.getShape())
                                    .sizeRange(stone.getSizeMm())
                                    .colorCategory(stone.getColorCategory() != null ? stone.getColorCategory().name() : null)
                                    .clarityGrade(stone.getClarity())
                                    .totalCaratWeight(caratWeight)
                                    .totalQuantity(stoneQty)
                                    .orderCount(1)
                                    .estimatedCost(cost)
                                    .build();
                        } else {
                            existing.setTotalCaratWeight(existing.getTotalCaratWeight().add(caratWeight));
                            existing.setTotalQuantity(existing.getTotalQuantity() + stoneQty);
                            existing.setOrderCount(existing.getOrderCount() + 1);
                            existing.setEstimatedCost(existing.getEstimatedCost().add(cost));
                            return existing;
                        }
                    });

                    if (stone.getTotalPrice() != null) {
                        totalStoneCost = totalStoneCost.add(stone.getTotalPrice().multiply(BigDecimal.valueOf(qty)));
                    }
                }
            }

            // Sum labor costs
            if (jtrc.getLaborComponents() != null) {
                for (JTRCLaborComponent labor : jtrc.getLaborComponents()) {
                    if (labor.getCost() != null) {
                        totalLaborCost = totalLaborCost.add(labor.getCost().multiply(BigDecimal.valueOf(qty)));
                    }
                }
            }
        }

        return MaterialAggregationSummary.builder()
                .metals(new ArrayList<>(metalMap.values()))
                .stones(new ArrayList<>(stoneMap.values()))
                .totalMetalCost(totalMetalCost)
                .totalStoneCost(totalStoneCost)
                .totalLaborCost(totalLaborCost)
                .grandTotal(totalMetalCost.add(totalStoneCost).add(totalLaborCost))
                .build();
    }
}
