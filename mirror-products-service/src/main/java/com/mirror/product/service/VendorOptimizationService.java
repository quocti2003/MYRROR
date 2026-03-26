package com.mirror.product.service;

import com.mirror.product.dto.VendorOptimizationRequest;
import com.mirror.product.dto.VendorOptimizationResponse;
import com.mirror.product.entity.Vendor;
import com.mirror.product.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for vendor optimization using multi-criteria decision analysis
 * Optimizes vendor selection based on cost, quality, and timeline constraints
 */
@Service
public class VendorOptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(VendorOptimizationService.class);

    @Autowired
    private VendorRepository vendorRepository;

    /**
     * Find optimal vendors based on multi-criteria optimization
     */
    public List<VendorOptimizationResponse> findOptimalVendors(VendorOptimizationRequest request) {
        try {
            // Get all vendors
            List<Vendor> allVendors = vendorRepository.findAll();

            // Calculate optimization score for each vendor
            List<VendorOptimizationResponse> optimizations = allVendors.stream()
                .map(vendor -> optimizeVendor(vendor, request))
                .filter(opt -> opt.getIsFeasible()) // Only include feasible options
                .sorted((a, b) -> b.getOptimizationScore().compareTo(a.getOptimizationScore()))
                .limit(10) // Return top 10
                .collect(Collectors.toList());

            logger.info("Found {} optimal vendors for request (budget: {}, pieces: {})",
                optimizations.size(), request.getMaxBudget(), request.getTotalPieces());

            return optimizations;

        } catch (Exception e) {
            logger.error("Error finding optimal vendors: {}", e.getMessage());
            throw new RuntimeException("Failed to find optimal vendors: " + e.getMessage(), e);
        }
    }

    /**
     * Optimize a single vendor against requirements
     */
    private VendorOptimizationResponse optimizeVendor(Vendor vendor, VendorOptimizationRequest request) {
        VendorOptimizationResponse response = new VendorOptimizationResponse();

        // Basic info
        response.setVendorId(Long.parseLong(vendor.getId().replace("VEN", "")));
        response.setVendorName(vendor.getName());
        response.setCountry(vendor.getCountry());
        response.setLeadTimeDays(vendor.getProductionLeadTimeDays());

        // Calculate total cost with breakdown
        VendorOptimizationResponse.CostBreakdown costBreakdown = calculateCostBreakdown(vendor, request);
        response.setCostBreakdown(costBreakdown);
        response.setTotalCost(costBreakdown.getTotalCost());
        response.setCostPerUnit(costBreakdown.getTotalCost()
            .divide(BigDecimal.valueOf(request.getTotalPieces()), 2, RoundingMode.HALF_UP));

        // Check budget constraint
        boolean meetsBudget = costBreakdown.getTotalCost().compareTo(request.getMaxBudget()) <= 0;
        response.setMeetsBudget(meetsBudget);

        // Check timeline constraint
        boolean meetsDeadline = checkDeadline(vendor, request.getDeadline());
        response.setMeetsDeadline(meetsDeadline);

        // Overall feasibility
        response.setIsFeasible(meetsBudget && meetsDeadline);

        // Calculate optimization score
        BigDecimal optimizationScore = calculateOptimizationScore(
            vendor, request, costBreakdown, meetsBudget, meetsDeadline);
        response.setOptimizationScore(optimizationScore);

        // Analyze strengths and concerns
        response.setStrengths(analyzeStrengths(vendor, request, costBreakdown));
        response.setConcerns(analyzeConcerns(vendor, request, costBreakdown));

        // Generate recommendation
        response.setRecommendation(generateRecommendation(response));

        return response;
    }

    /**
     * Calculate detailed cost breakdown
     */
    private VendorOptimizationResponse.CostBreakdown calculateCostBreakdown(
            Vendor vendor, VendorOptimizationRequest request) {

        VendorOptimizationResponse.CostBreakdown breakdown = new VendorOptimizationResponse.CostBreakdown();

        // Material cost (avg product cost * quantity)
        BigDecimal materialCost = (vendor.getAvgProductCost() != null)
            ? vendor.getAvgProductCost().multiply(BigDecimal.valueOf(request.getTotalPieces()))
            : BigDecimal.ZERO;
        breakdown.setMaterialCost(materialCost);

        // Labor cost
        BigDecimal laborCost = (vendor.getAvgLaborCostPerPiece() != null)
            ? vendor.getAvgLaborCostPerPiece().multiply(BigDecimal.valueOf(request.getTotalPieces()))
            : BigDecimal.ZERO;
        breakdown.setLaborCost(laborCost);

        // Shipping cost
        BigDecimal shippingCost = (vendor.getShippingFee() != null)
            ? vendor.getShippingFee()
            : BigDecimal.ZERO;
        breakdown.setShippingCost(shippingCost);

        // Subtotal before taxes
        BigDecimal subtotal = materialCost.add(laborCost).add(shippingCost);

        // Customs duty
        BigDecimal customsDuty = (vendor.getTaxCustomsPercent() != null)
            ? subtotal.multiply(vendor.getTaxCustomsPercent().divide(BigDecimal.valueOf(100)))
            : BigDecimal.ZERO;
        breakdown.setCustomsDuty(customsDuty);

        // VAT
        BigDecimal taxableAmount = subtotal.add(customsDuty);
        BigDecimal vat = (vendor.getVatTaxPercent() != null)
            ? taxableAmount.multiply(vendor.getVatTaxPercent().divide(BigDecimal.valueOf(100)))
            : BigDecimal.ZERO;
        breakdown.setVat(vat);

        // Total cost
        BigDecimal totalCost = taxableAmount.add(vat).setScale(2, RoundingMode.HALF_UP);
        breakdown.setTotalCost(totalCost);

        return breakdown;
    }

    /**
     * Check if vendor can meet deadline
     */
    private boolean checkDeadline(Vendor vendor, String deadlineStr) {
        if (vendor.getProductionLeadTimeDays() == null || deadlineStr == null) {
            return true; // Assume ok if no data
        }

        try {
            LocalDate deadline = LocalDate.parse(deadlineStr, DateTimeFormatter.ISO_DATE);
            LocalDate today = LocalDate.now();
            long daysUntilDeadline = ChronoUnit.DAYS.between(today, deadline);

            return daysUntilDeadline >= vendor.getProductionLeadTimeDays();
        } catch (Exception e) {
            logger.warn("Error parsing deadline: {}", deadlineStr);
            return true; // Assume ok if can't parse
        }
    }

    /**
     * Calculate multi-criteria optimization score
     */
    private BigDecimal calculateOptimizationScore(
            Vendor vendor, VendorOptimizationRequest request,
            VendorOptimizationResponse.CostBreakdown costBreakdown,
            boolean meetsBudget, boolean meetsDeadline) {

        // If not feasible, score is very low
        if (!meetsBudget || !meetsDeadline) {
            return BigDecimal.valueOf(10);
        }

        // Cost score (0-100) - how much under budget
        BigDecimal costScore;
        if (costBreakdown.getTotalCost().compareTo(BigDecimal.ZERO) == 0) {
            costScore = BigDecimal.valueOf(50);
        } else {
            BigDecimal costRatio = costBreakdown.getTotalCost()
                .divide(request.getMaxBudget(), 4, RoundingMode.HALF_UP);
            costScore = BigDecimal.valueOf(100).subtract(costRatio.multiply(BigDecimal.valueOf(100)))
                .max(BigDecimal.ZERO);
        }

        // Quality score (0-100)
        BigDecimal qualityScore = calculateQualityScore(vendor);

        // Timeline score (0-100)
        BigDecimal timelineScore = calculateTimelineScore(vendor);

        // Weighted average
        BigDecimal optimizationScore = costScore.multiply(request.getCostWeight())
            .add(qualityScore.multiply(request.getQualityWeight()))
            .add(timelineScore.multiply(request.getTimelineWeight()))
            .setScale(2, RoundingMode.HALF_UP);

        return optimizationScore.min(BigDecimal.valueOf(100));
    }

    /**
     * Calculate quality score
     */
    private BigDecimal calculateQualityScore(Vendor vendor) {
        BigDecimal score = BigDecimal.valueOf(50); // Base score

        // Established vendor bonus
        if (vendor.getLastQuoteDate() != null) {
            score = score.add(BigDecimal.valueOf(20));
        }

        // Contact info completeness
        if (vendor.getContactPerson() != null && vendor.getContactEmail() != null) {
            score = score.add(BigDecimal.valueOf(15));
        }

        // Has commission terms (partnership indicator)
        if (vendor.getCommissionTerm() != null && !vendor.getCommissionTerm().isEmpty()) {
            score = score.add(BigDecimal.valueOf(15));
        }

        return score.min(BigDecimal.valueOf(100));
    }

    /**
     * Calculate timeline score
     */
    private BigDecimal calculateTimelineScore(Vendor vendor) {
        if (vendor.getProductionLeadTimeDays() == null) {
            return BigDecimal.valueOf(50);
        }

        Integer leadTime = vendor.getProductionLeadTimeDays();

        if (leadTime <= 30) {
            return BigDecimal.valueOf(100);
        } else if (leadTime <= 60) {
            return BigDecimal.valueOf(80);
        } else if (leadTime <= 90) {
            return BigDecimal.valueOf(60);
        } else {
            return BigDecimal.valueOf(40);
        }
    }

    /**
     * Analyze vendor strengths
     */
    private List<String> analyzeStrengths(
            Vendor vendor, VendorOptimizationRequest request,
            VendorOptimizationResponse.CostBreakdown costBreakdown) {

        List<String> strengths = new ArrayList<>();

        // Under budget
        if (costBreakdown.getTotalCost().compareTo(request.getMaxBudget()) < 0) {
            BigDecimal savings = request.getMaxBudget().subtract(costBreakdown.getTotalCost());
            strengths.add("Under budget by " + savings.setScale(2, RoundingMode.HALF_UP));
        }

        // Fast lead time
        if (vendor.getProductionLeadTimeDays() != null && vendor.getProductionLeadTimeDays() <= 45) {
            strengths.add("Fast production time (" + vendor.getProductionLeadTimeDays() + " days)");
        }

        // Competitive pricing
        if (vendor.getAvgProductCost() != null && vendor.getAvgProductCost().compareTo(BigDecimal.valueOf(50)) <= 0) {
            strengths.add("Competitive unit pricing");
        }

        // Good payment terms
        if (vendor.getPaymentTerms() != null && !vendor.getPaymentTerms().isEmpty()) {
            strengths.add("Flexible payment terms available");
        }

        return strengths;
    }

    /**
     * Analyze vendor concerns
     */
    private List<String> analyzeConcerns(
            Vendor vendor, VendorOptimizationRequest request,
            VendorOptimizationResponse.CostBreakdown costBreakdown) {

        List<String> concerns = new ArrayList<>();

        // Over budget
        if (costBreakdown.getTotalCost().compareTo(request.getMaxBudget()) > 0) {
            BigDecimal overage = costBreakdown.getTotalCost().subtract(request.getMaxBudget());
            concerns.add("Over budget by " + overage.setScale(2, RoundingMode.HALF_UP));
        }

        // Slow lead time
        if (vendor.getProductionLeadTimeDays() != null && vendor.getProductionLeadTimeDays() > 90) {
            concerns.add("Long production lead time (" + vendor.getProductionLeadTimeDays() + " days)");
        }

        // Missing cost data
        if (vendor.getAvgProductCost() == null) {
            concerns.add("No pricing history available");
        }

        // High shipping
        if (vendor.getShippingFee() != null && vendor.getShippingFee().compareTo(BigDecimal.valueOf(500)) > 0) {
            concerns.add("High shipping costs");
        }

        return concerns;
    }

    /**
     * Generate recommendation text
     */
    private String generateRecommendation(VendorOptimizationResponse response) {
        if (!response.getIsFeasible()) {
            return "Not recommended - does not meet budget or timeline constraints";
        }

        if (response.getOptimizationScore().compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "Highly recommended - excellent fit for requirements";
        } else if (response.getOptimizationScore().compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "Recommended - good fit with minor trade-offs";
        } else {
            return "Consider as alternative - meets basic requirements";
        }
    }
}
