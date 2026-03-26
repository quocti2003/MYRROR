package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.entity.DesignProduct;
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
public class DesignProductResponse {

    private String id;
    private String designerId;
    private String designerName;
    private String productId;
    private String productName;
    private BigDecimal commissionPercentage;
    private BigDecimal loyaltyPercentage;
    private String designName;
    private String designDescription;
    private String designConcept;
    private String designInspiration;
    private LocalDateTime designCreatedDate;
    private LocalDateTime productConversionDate;
    private Integer totalSalesCount;
    private BigDecimal totalSalesAmount;
    private BigDecimal totalCommissionEarned;
    private BigDecimal totalLoyaltyEarned;
    private String designStatus;
    private Boolean featuredDesign;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    public DesignProductResponse(DesignProduct designProduct) {
        this.id = designProduct.getId();
        this.designerId = designProduct.getDesigner() != null ? designProduct.getDesigner().getId() : null;
        this.designerName = designProduct.getDesigner() != null ? designProduct.getDesigner().getName() : null;
        this.productId = designProduct.getProduct() != null ? designProduct.getProduct().getId() : null;
        this.productName = designProduct.getProduct() != null ? designProduct.getProduct().getItemName() : null;
        this.commissionPercentage = designProduct.getCommissionPercentage();
        this.loyaltyPercentage = designProduct.getLoyaltyPercentage();
        this.designName = designProduct.getDesignName();
        this.designDescription = designProduct.getDesignDescription();
        this.designConcept = designProduct.getDesignConcept();
        this.designInspiration = designProduct.getDesignInspiration();
        this.designCreatedDate = designProduct.getDesignCreatedDate();
        this.productConversionDate = designProduct.getProductConversionDate();
        this.totalSalesCount = designProduct.getTotalSalesCount();
        this.totalSalesAmount = designProduct.getTotalSalesAmount();
        this.totalCommissionEarned = designProduct.getTotalCommissionEarned();
        this.totalLoyaltyEarned = designProduct.getTotalLoyaltyEarned();
        this.designStatus = designProduct.getDesignStatus();
        this.featuredDesign = designProduct.getFeaturedDesign();
        this.averageRating = designProduct.getAverageRating();
        this.reviewCount = designProduct.getReviewCount();
        this.isActive = designProduct.getIsActive();
        this.createdAt = designProduct.getCreatedAt();
        this.updatedAt = designProduct.getUpdatedAt();
    }
}