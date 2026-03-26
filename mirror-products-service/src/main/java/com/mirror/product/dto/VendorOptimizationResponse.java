package com.mirror.product.dto;

import java.math.BigDecimal;
import java.util.List;

public class VendorOptimizationResponse {
    private Long vendorId;
    private String vendorName;
    private String country;
    private BigDecimal optimizationScore; // 0-100
    private BigDecimal totalCost;
    private BigDecimal costPerUnit;
    private Integer leadTimeDays;
    private Boolean meetsBudget;
    private Boolean meetsDeadline;
    private Boolean isFeasible;
    private CostBreakdown costBreakdown;
    private List<String> strengths;
    private List<String> concerns;
    private String recommendation;

    public VendorOptimizationResponse() {}

    // Inner class for cost breakdown
    public static class CostBreakdown {
        private BigDecimal materialCost;
        private BigDecimal laborCost;
        private BigDecimal shippingCost;
        private BigDecimal customsDuty;
        private BigDecimal vat;
        private BigDecimal totalCost;

        public CostBreakdown() {}

        // Getters and setters
        public BigDecimal getMaterialCost() {
            return materialCost;
        }

        public void setMaterialCost(BigDecimal materialCost) {
            this.materialCost = materialCost;
        }

        public BigDecimal getLaborCost() {
            return laborCost;
        }

        public void setLaborCost(BigDecimal laborCost) {
            this.laborCost = laborCost;
        }

        public BigDecimal getShippingCost() {
            return shippingCost;
        }

        public void setShippingCost(BigDecimal shippingCost) {
            this.shippingCost = shippingCost;
        }

        public BigDecimal getCustomsDuty() {
            return customsDuty;
        }

        public void setCustomsDuty(BigDecimal customsDuty) {
            this.customsDuty = customsDuty;
        }

        public BigDecimal getVat() {
            return vat;
        }

        public void setVat(BigDecimal vat) {
            this.vat = vat;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
        }
    }

    // Getters and setters for main class
    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public BigDecimal getOptimizationScore() {
        return optimizationScore;
    }

    public void setOptimizationScore(BigDecimal optimizationScore) {
        this.optimizationScore = optimizationScore;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getCostPerUnit() {
        return costPerUnit;
    }

    public void setCostPerUnit(BigDecimal costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public Boolean getMeetsBudget() {
        return meetsBudget;
    }

    public void setMeetsBudget(Boolean meetsBudget) {
        this.meetsBudget = meetsBudget;
    }

    public Boolean getMeetsDeadline() {
        return meetsDeadline;
    }

    public void setMeetsDeadline(Boolean meetsDeadline) {
        this.meetsDeadline = meetsDeadline;
    }

    public Boolean getIsFeasible() {
        return isFeasible;
    }

    public void setIsFeasible(Boolean isFeasible) {
        this.isFeasible = isFeasible;
    }

    public CostBreakdown getCostBreakdown() {
        return costBreakdown;
    }

    public void setCostBreakdown(CostBreakdown costBreakdown) {
        this.costBreakdown = costBreakdown;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getConcerns() {
        return concerns;
    }

    public void setConcerns(List<String> concerns) {
        this.concerns = concerns;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
