package com.mirror.product.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class VendorMatchRequest {
    @NotNull(message = "Collection plan ID is required")
    private Long collectionPlanId;

    private String productCategory;
    private String material;
    private Integer minQuantity;
    private BigDecimal maxBudget;
    private String deadline;
    private List<String> requiredCapabilities;

    public VendorMatchRequest() {}

    // Getters and setters
    public Long getCollectionPlanId() {
        return collectionPlanId;
    }

    public void setCollectionPlanId(Long collectionPlanId) {
        this.collectionPlanId = collectionPlanId;
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

    public Integer getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
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

    public List<String> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public void setRequiredCapabilities(List<String> requiredCapabilities) {
        this.requiredCapabilities = requiredCapabilities;
    }
}
