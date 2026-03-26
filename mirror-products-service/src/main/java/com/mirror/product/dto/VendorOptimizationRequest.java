package com.mirror.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class VendorOptimizationRequest {
    @NotNull(message = "Total pieces is required")
    @Positive(message = "Total pieces must be positive")
    private Integer totalPieces;

    @NotNull(message = "Max budget is required")
    @Positive(message = "Max budget must be positive")
    private BigDecimal maxBudget;

    @NotNull(message = "Deadline is required")
    private String deadline; // ISO date format

    private String productCategory;
    private String material;
    private Integer qualityRequirement; // 1-5 scale

    // Weight factors for optimization (0-1)
    private BigDecimal costWeight; // default 0.40
    private BigDecimal qualityWeight; // default 0.35
    private BigDecimal timelineWeight; // default 0.25

    public VendorOptimizationRequest() {
        // Default weights
        this.costWeight = BigDecimal.valueOf(0.40);
        this.qualityWeight = BigDecimal.valueOf(0.35);
        this.timelineWeight = BigDecimal.valueOf(0.25);
    }

    // Getters and setters
    public Integer getTotalPieces() {
        return totalPieces;
    }

    public void setTotalPieces(Integer totalPieces) {
        this.totalPieces = totalPieces;
    }

    public BigDecimal getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(BigDecimal maxBudget) {
        this.maxBudget = maxBudget;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public Integer getQualityRequirement() {
        return qualityRequirement;
    }

    public void setQualityRequirement(Integer qualityRequirement) {
        this.qualityRequirement = qualityRequirement;
    }

    public BigDecimal getCostWeight() {
        return costWeight;
    }

    public void setCostWeight(BigDecimal costWeight) {
        this.costWeight = costWeight;
    }

    public BigDecimal getQualityWeight() {
        return qualityWeight;
    }

    public void setQualityWeight(BigDecimal qualityWeight) {
        this.qualityWeight = qualityWeight;
    }

    public BigDecimal getTimelineWeight() {
        return timelineWeight;
    }

    public void setTimelineWeight(BigDecimal timelineWeight) {
        this.timelineWeight = timelineWeight;
    }
}
