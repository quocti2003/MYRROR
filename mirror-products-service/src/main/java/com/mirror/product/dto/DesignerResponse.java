package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.entity.Designer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DesignerResponse {

    private String id;
    private String code;
    private String name;
    private String ownerUserId;
    private String brandName;
    private String specialty;
    private Integer yearsExperience;
    private String designStyle;
    private BigDecimal defaultCommissionPercent;
    private BigDecimal defaultLoyaltyPercent;
    private Integer totalDesignsCreated;
    private Integer totalProductsSold;
    private BigDecimal totalEarnings;
    private LocalDateTime joinDate;
    private LocalDateTime lastDesignDate;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private String socialMediaLinks;
    private String bio;
    private String portfolioUrl;
    private Boolean verified;
    private Boolean featured;
    private BigDecimal rating;
    private Integer reviewCount;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer designProductCount;

    public DesignerResponse(Designer designer) {
        this.id = designer.getId();
        this.code = designer.getCode();
        this.name = designer.getName();
        this.ownerUserId = designer.getOwnerUserId();
        this.brandName = designer.getBrandName();
        this.specialty = designer.getSpecialty();
        this.yearsExperience = designer.getYearsExperience();
        this.designStyle = designer.getDesignStyle();
        this.defaultCommissionPercent = designer.getDefaultCommissionPercent();
        this.defaultLoyaltyPercent = designer.getDefaultLoyaltyPercent();
        this.totalDesignsCreated = designer.getTotalDesignsCreated();
        this.totalProductsSold = designer.getTotalProductsSold();
        this.totalEarnings = designer.getTotalEarnings();
        this.joinDate = designer.getJoinDate();
        this.lastDesignDate = designer.getLastDesignDate();
        this.contactEmail = designer.getContactEmail();
        this.contactPhone = designer.getContactPhone();
        this.website = designer.getWebsite();
        this.socialMediaLinks = designer.getSocialMediaLinks();
        this.bio = designer.getBio();
        this.portfolioUrl = designer.getPortfolioUrl();
        this.verified = designer.getVerified();
        this.featured = designer.getFeatured();
        this.rating = designer.getRating();
        this.reviewCount = designer.getReviewCount();
        this.isActive = designer.getIsActive();
        this.createdAt = designer.getCreatedAt();
        this.updatedAt = designer.getUpdatedAt();

        if (designer.getDesignProducts() != null) {
            this.designProductCount = designer.getDesignProducts().size();
        }
    }
}