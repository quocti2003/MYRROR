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
@Table(name = "design_sale_transactions")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DesignSaleTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_product_id", nullable = false)
    private DesignProduct designProduct;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "sale_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal saleAmount;

    @Column(name = "commission_amount", precision = 15, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "loyalty_amount", precision = 15, scale = 2)
    private BigDecimal loyaltyAmount = BigDecimal.ZERO;

    @Column(name = "commission_percentage", precision = 5, scale = 2)
    private BigDecimal commissionPercentage = BigDecimal.ZERO;

    @Column(name = "loyalty_percentage", precision = 5, scale = 2)
    private BigDecimal loyaltyPercentage = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "sale_date", nullable = false)
    private LocalDateTime saleDate;

    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @Column(name = "customer_rating", precision = 3, scale = 2)
    private BigDecimal customerRating;

    @Column(name = "customer_review", columnDefinition = "TEXT")
    private String customerReview;

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.DST)); // Design Sale Transaction
        }
        if (saleDate == null) {
            saleDate = LocalDateTime.now();
        }
    }

    public BigDecimal getTotalEarnings() {
        BigDecimal commission = commissionAmount != null ? commissionAmount : BigDecimal.ZERO;
        BigDecimal loyalty = loyaltyAmount != null ? loyaltyAmount : BigDecimal.ZERO;
        return commission.add(loyalty);
    }

    public void calculateCommissionAmounts() {
        if (saleAmount != null && designProduct != null) {
            BigDecimal commissionPerc = designProduct.getCommissionPercentage() != null
                ? designProduct.getCommissionPercentage() : BigDecimal.ZERO;
            BigDecimal loyaltyPerc = designProduct.getLoyaltyPercentage() != null
                ? designProduct.getLoyaltyPercentage() : BigDecimal.ZERO;

            this.commissionPercentage = commissionPerc;
            this.loyaltyPercentage = loyaltyPerc;

            this.commissionAmount = saleAmount.multiply(commissionPerc).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            this.loyaltyAmount = saleAmount.multiply(loyaltyPerc).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
        }
    }
}