package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.mirror.product.util.SequenceIdGenerator;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.config.ApplicationContextProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "design_products")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DesignProduct extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designer_id", nullable = false)
    private Designer designer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private MirrorProduct product;

    // Commission and Loyalty rates for this specific design
    @Column(name = "commission_percentage", precision = 5, scale = 2)
    private BigDecimal commissionPercentage;

    @Column(name = "loyalty_percentage", precision = 5, scale = 2)
    private BigDecimal loyaltyPercentage;

    // Design specific information
    @Column(name = "design_name")
    private String designName;

    @Column(name = "design_description", columnDefinition = "TEXT")
    private String designDescription;

    @Column(name = "design_concept", columnDefinition = "TEXT")
    private String designConcept;

    @Column(name = "design_inspiration")
    private String designInspiration;

    // Original design creation date
    @Column(name = "design_created_date")
    private LocalDateTime designCreatedDate;

    // When this design was converted to a product
    @Column(name = "product_conversion_date")
    private LocalDateTime productConversionDate;

    // Sales tracking
    @Column(name = "total_sales_count")
    private Integer totalSalesCount = 0;

    @Column(name = "total_sales_amount", precision = 15, scale = 2)
    private BigDecimal totalSalesAmount = BigDecimal.ZERO;

    @Column(name = "total_commission_earned", precision = 15, scale = 2)
    private BigDecimal totalCommissionEarned = BigDecimal.ZERO;

    @Column(name = "total_loyalty_earned", precision = 15, scale = 2)
    private BigDecimal totalLoyaltyEarned = BigDecimal.ZERO;

    // Design status
    @Column(name = "design_status")
    private String designStatus = "ACTIVE"; // ACTIVE, DISCONTINUED, SEASONAL

    // Featured status for showcase
    @Column(name = "featured_design")
    private Boolean featuredDesign = false;

    // Customer satisfaction
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.DPR));
        }
        if (productConversionDate == null) {
            productConversionDate = LocalDateTime.now();
        }
    }

    // Helper methods
    public BigDecimal getTotalEarnings() {
        BigDecimal commission = totalCommissionEarned != null ? totalCommissionEarned : BigDecimal.ZERO;
        BigDecimal loyalty = totalLoyaltyEarned != null ? totalLoyaltyEarned : BigDecimal.ZERO;
        return commission.add(loyalty);
    }

    public BigDecimal getTotalCommissionPercentage() {
        BigDecimal commission = commissionPercentage != null ? commissionPercentage : BigDecimal.ZERO;
        BigDecimal loyalty = loyaltyPercentage != null ? loyaltyPercentage : BigDecimal.ZERO;
        return commission.add(loyalty);
    }

    public boolean isValidPercentage() {
        return getTotalCommissionPercentage().compareTo(new BigDecimal("100.00")) <= 0;
    }

    public boolean isPopularDesign() {
        return totalSalesCount != null && totalSalesCount >= 10;
    }

    public BigDecimal getAverageOrderValue() {
        if (totalSalesCount != null && totalSalesCount > 0 && totalSalesAmount != null) {
            return totalSalesAmount.divide(BigDecimal.valueOf(totalSalesCount), 2, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}