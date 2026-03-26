package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.mirror.product.util.SequenceIdGenerator;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.config.ApplicationContextProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "designers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Designer extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code; // Designer code (e.g., DES001)

    @Column(nullable = false)
    private String name;

    // User relationship - links designer to user from User Service
    @Column(name = "owner_user_id")
    private String ownerUserId; // User ID from User Service JWT token

    // Designer Profile Information
    @Column(name = "brand_name")
    private String brandName; // Designer's brand/studio name

    @Column(name = "specialty")
    private String specialty; // e.g., "Fine Jewelry", "Engagement Rings", etc.

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "design_style")
    private String designStyle; // e.g., "Modern", "Classic", "Vintage"

    // Commission Structure
    @Column(name = "default_commission_percent", precision = 5, scale = 2)
    private BigDecimal defaultCommissionPercent;

    @Column(name = "default_loyalty_percent", precision = 5, scale = 2)
    private BigDecimal defaultLoyaltyPercent;

    // Portfolio Information
    @Column(name = "total_designs_created")
    private Integer totalDesignsCreated;

    @Column(name = "total_products_sold")
    private Integer totalProductsSold;

    @Column(name = "total_earnings", precision = 15, scale = 2)
    private BigDecimal totalEarnings;

    @Column(name = "join_date")
    private LocalDateTime joinDate;

    @Column(name = "last_design_date")
    private LocalDateTime lastDesignDate;

    // Contact Information
    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "website")
    private String website;

    @Column(name = "social_media_links", columnDefinition = "TEXT")
    private String socialMediaLinks; // JSON string for multiple social links

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    // Status and Rating
    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "featured")
    private Boolean featured = false;

    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating; // Average rating from customers

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    // Relationships
    @OneToMany(mappedBy = "designer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DesignProduct> designProducts;

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.DES));
        }
        if (joinDate == null) {
            joinDate = LocalDateTime.now();
        }
    }

    // Helper methods
    public BigDecimal getAverageEarningsPerSale() {
        if (totalProductsSold != null && totalProductsSold > 0 && totalEarnings != null) {
            return totalEarnings.divide(BigDecimal.valueOf(totalProductsSold), 2, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public boolean isActive() {
        return getIsActive() != null && getIsActive();
    }

    public boolean isVerifiedDesigner() {
        return verified != null && verified;
    }
}