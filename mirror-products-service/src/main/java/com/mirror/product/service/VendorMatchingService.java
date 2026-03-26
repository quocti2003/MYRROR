package com.mirror.product.service;

import com.mirror.product.dto.VendorMatchRequest;
import com.mirror.product.dto.VendorMatchResponse;
import com.mirror.product.entity.Vendor;
import com.mirror.product.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for vendor matching and selection
 * Uses internal scoring algorithm based on quality, price, and timeline
 */
@Service
public class VendorMatchingService {

    private static final Logger logger = LoggerFactory.getLogger(VendorMatchingService.class);

    @Autowired
    private VendorRepository vendorRepository;

    /**
     * Find matching vendors based on requirements
     */
    public List<VendorMatchResponse> findMatchingVendors(VendorMatchRequest request) {
        try {
            // Get all active vendors
            List<Vendor> allVendors = vendorRepository.findAll();

            // Calculate match score for each vendor
            List<VendorMatchResponse> matches = allVendors.stream()
                .map(vendor -> calculateVendorMatch(vendor, request))
                .filter(match -> match.getMatchScore().compareTo(BigDecimal.ZERO) > 0)
                .sorted((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()))
                .collect(Collectors.toList());

            // Mark top 3 as recommended
            for (int i = 0; i < Math.min(3, matches.size()); i++) {
                matches.get(i).setRecommended(true);
            }

            logger.info("Found {} matching vendors for collection plan ID: {}",
                matches.size(), request.getCollectionPlanId());

            return matches;

        } catch (Exception e) {
            logger.error("Error finding matching vendors: {}", e.getMessage());
            throw new RuntimeException("Failed to find matching vendors: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate match score for a single vendor
     */
    private VendorMatchResponse calculateVendorMatch(Vendor vendor, VendorMatchRequest request) {
        VendorMatchResponse match = new VendorMatchResponse();

        // Basic vendor info
        match.setVendorId(Long.parseLong(vendor.getId().replace("VEN", "")));
        match.setVendorName(vendor.getName());
        match.setCountry(vendor.getCountry());
        match.setEstimatedCostPerUnit(vendor.getAvgProductCost());
        match.setLeadTimeDays(vendor.getProductionLeadTimeDays());
        match.setCurrency("USD"); // Assume USD for now

        // Parse specializations and certifications (if stored as comma-separated in vendor)
        match.setSpecializations(parseSpecializations(vendor));
        match.setCertifications(parseCertifications(vendor));

        // Calculate individual scores
        BigDecimal qualityScore = calculateQualityScore(vendor, request);
        BigDecimal priceScore = calculatePriceScore(vendor, request);
        BigDecimal timelineScore = calculateTimelineScore(vendor, request);

        match.setQualityScore(qualityScore);
        match.setPriceScore(priceScore);
        match.setTimelineScore(timelineScore);

        // Overall match score (weighted average)
        BigDecimal matchScore = qualityScore.multiply(BigDecimal.valueOf(0.40))
            .add(priceScore.multiply(BigDecimal.valueOf(0.35)))
            .add(timelineScore.multiply(BigDecimal.valueOf(0.25)))
            .setScale(2, RoundingMode.HALF_UP);

        match.setMatchScore(matchScore);

        // Add notes
        match.setNotes(generateMatchNotes(vendor, request));

        return match;
    }

    /**
     * Calculate quality score (0-100)
     */
    private BigDecimal calculateQualityScore(Vendor vendor, VendorMatchRequest request) {
        BigDecimal score = BigDecimal.valueOf(50); // Base score

        // Higher score for established vendors with track record
        if (vendor.getLastQuoteDate() != null) {
            score = score.add(BigDecimal.valueOf(20));
        }

        // Bonus for having commission terms (indicates partnership)
        if (vendor.getCommissionTerm() != null && !vendor.getCommissionTerm().isEmpty()) {
            score = score.add(BigDecimal.valueOf(10));
        }

        // Bonus for having detailed contact info
        if (vendor.getContactPerson() != null && vendor.getContactEmail() != null) {
            score = score.add(BigDecimal.valueOf(10));
        }

        // Country-specific quality adjustments (example logic)
        if ("Thailand".equalsIgnoreCase(vendor.getCountry()) ||
            "Vietnam".equalsIgnoreCase(vendor.getCountry())) {
            score = score.add(BigDecimal.valueOf(10));
        }

        return score.min(BigDecimal.valueOf(100));
    }

    /**
     * Calculate price score (0-100) - higher is better (more competitive)
     */
    private BigDecimal calculatePriceScore(Vendor vendor, VendorMatchRequest request) {
        if (vendor.getAvgProductCost() == null) {
            return BigDecimal.valueOf(50); // Default score if no price data
        }

        BigDecimal avgCost = vendor.getAvgProductCost();

        // If budget is specified, score based on how well it fits
        if (request.getMaxBudget() != null) {
            BigDecimal budgetRatio = avgCost.divide(request.getMaxBudget(), 4, RoundingMode.HALF_UP);

            if (budgetRatio.compareTo(BigDecimal.valueOf(0.7)) <= 0) {
                return BigDecimal.valueOf(100); // Well under budget
            } else if (budgetRatio.compareTo(BigDecimal.valueOf(0.9)) <= 0) {
                return BigDecimal.valueOf(80); // Under budget
            } else if (budgetRatio.compareTo(BigDecimal.valueOf(1.0)) <= 0) {
                return BigDecimal.valueOf(60); // At budget
            } else {
                return BigDecimal.valueOf(30); // Over budget
            }
        }

        // If no budget specified, score based on absolute cost
        if (avgCost.compareTo(BigDecimal.valueOf(50)) <= 0) {
            return BigDecimal.valueOf(90);
        } else if (avgCost.compareTo(BigDecimal.valueOf(100)) <= 0) {
            return BigDecimal.valueOf(70);
        } else {
            return BigDecimal.valueOf(50);
        }
    }

    /**
     * Calculate timeline score (0-100)
     */
    private BigDecimal calculateTimelineScore(Vendor vendor, VendorMatchRequest request) {
        if (vendor.getProductionLeadTimeDays() == null) {
            return BigDecimal.valueOf(50); // Default score
        }

        Integer leadTime = vendor.getProductionLeadTimeDays();

        // Score based on lead time
        if (leadTime <= 30) {
            return BigDecimal.valueOf(100); // Fast production
        } else if (leadTime <= 60) {
            return BigDecimal.valueOf(80); // Moderate production
        } else if (leadTime <= 90) {
            return BigDecimal.valueOf(60); // Slower production
        } else {
            return BigDecimal.valueOf(40); // Very slow
        }
    }

    /**
     * Parse specializations from vendor data
     * TODO: This should be a proper JSON field or separate table
     */
    private List<String> parseSpecializations(Vendor vendor) {
        List<String> specializations = new ArrayList<>();

        // For now, infer from vendor type and country
        if (vendor.getVendorType() != null) {
            specializations.add(vendor.getVendorType().toString());
        }

        if ("Thailand".equalsIgnoreCase(vendor.getCountry())) {
            specializations.add("Gold Jewelry");
            specializations.add("Silver Jewelry");
        } else if ("China".equalsIgnoreCase(vendor.getCountry())) {
            specializations.add("Mass Production");
            specializations.add("Cost Efficiency");
        } else if ("Vietnam".equalsIgnoreCase(vendor.getCountry())) {
            specializations.add("Handcrafted");
            specializations.add("Custom Design");
        }

        return specializations;
    }

    /**
     * Parse certifications from vendor data
     * TODO: This should be a proper JSON field or separate table
     */
    private List<String> parseCertifications(Vendor vendor) {
        List<String> certifications = new ArrayList<>();

        // For now, add default certifications
        certifications.add("ISO 9001");

        if ("Thailand".equalsIgnoreCase(vendor.getCountry())) {
            certifications.add("Thai Gems & Jewelry");
        }

        return certifications;
    }

    /**
     * Generate match notes
     */
    private String generateMatchNotes(Vendor vendor, VendorMatchRequest request) {
        StringBuilder notes = new StringBuilder();

        if (vendor.getAvgProductCost() != null && request.getMaxBudget() != null) {
            BigDecimal diff = request.getMaxBudget().subtract(vendor.getAvgProductCost());
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                notes.append("Under budget by ").append(diff).append(". ");
            }
        }

        if (vendor.getProductionLeadTimeDays() != null) {
            notes.append("Lead time: ").append(vendor.getProductionLeadTimeDays()).append(" days. ");
        }

        if (vendor.getPaymentTerms() != null) {
            notes.append("Payment: ").append(vendor.getPaymentTerms()).append(".");
        }

        return notes.toString();
    }
}
