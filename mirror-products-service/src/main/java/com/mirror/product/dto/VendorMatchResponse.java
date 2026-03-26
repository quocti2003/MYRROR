package com.mirror.product.dto;

import java.math.BigDecimal;
import java.util.List;

public class VendorMatchResponse {
    private Long vendorId;
    private String vendorName;
    private String country;
    private BigDecimal matchScore; // 0-100
    private BigDecimal qualityScore;
    private BigDecimal priceScore;
    private BigDecimal timelineScore;
    private BigDecimal estimatedCostPerUnit;
    private Integer leadTimeDays;
    private Integer minOrderQuantity;
    private String currency;
    private List<String> specializations;
    private List<String> certifications;
    private String notes;
    private Boolean recommended;

    public VendorMatchResponse() {}

    // Getters and setters
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

    public BigDecimal getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(BigDecimal matchScore) {
        this.matchScore = matchScore;
    }

    public BigDecimal getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(BigDecimal qualityScore) {
        this.qualityScore = qualityScore;
    }

    public BigDecimal getPriceScore() {
        return priceScore;
    }

    public void setPriceScore(BigDecimal priceScore) {
        this.priceScore = priceScore;
    }

    public BigDecimal getTimelineScore() {
        return timelineScore;
    }

    public void setTimelineScore(BigDecimal timelineScore) {
        this.timelineScore = timelineScore;
    }

    public BigDecimal getEstimatedCostPerUnit() {
        return estimatedCostPerUnit;
    }

    public void setEstimatedCostPerUnit(BigDecimal estimatedCostPerUnit) {
        this.estimatedCostPerUnit = estimatedCostPerUnit;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public Integer getMinOrderQuantity() {
        return minOrderQuantity;
    }

    public void setMinOrderQuantity(Integer minOrderQuantity) {
        this.minOrderQuantity = minOrderQuantity;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<String> getSpecializations() {
        return specializations;
    }

    public void setSpecializations(List<String> specializations) {
        this.specializations = specializations;
    }

    public List<String> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<String> certifications) {
        this.certifications = certifications;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getRecommended() {
        return recommended;
    }

    public void setRecommended(Boolean recommended) {
        this.recommended = recommended;
    }
}
