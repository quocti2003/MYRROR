package com.mirror.product.entity.pod;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Entity representing a phygital partner's inventory for a specific product.
 * Tracks stock levels, pricing, and reorder thresholds.
 */
@Entity
@Table(name = "partner_inventory",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_partner_inventory_partner_product",
        columnNames = {"partner_id", "product_id"}),
    indexes = {
        @Index(name = "idx_partner_inventory_partner", columnList = "partner_id"),
        @Index(name = "idx_partner_inventory_low_stock", columnList = "partner_id,quantity_available")
    })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerInventory extends BaseEntity {

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "quantity_on_hand", nullable = false)
    @Builder.Default
    private Integer quantityOnHand = 0;

    @Column(name = "quantity_reserved", nullable = false)
    @Builder.Default
    private Integer quantityReserved = 0;

    @Column(name = "quantity_available", nullable = false)
    @Builder.Default
    private Integer quantityAvailable = 0;

    @Column(name = "wholesale_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal wholesalePrice;

    @Column(name = "partner_retail_price", precision = 15, scale = 2)
    private BigDecimal partnerRetailPrice;

    @Column(name = "mirror_retail_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal mirrorRetailPrice;

    @Column(name = "reorder_level", nullable = false)
    @Builder.Default
    private Integer reorderLevel = 5;

    @Column(name = "max_stock_level", nullable = false)
    @Builder.Default
    private Integer maxStockLevel = 50;

    @Column(name = "last_restocked_at")
    private Instant lastRestockedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    // === RELATIONSHIPS ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PodPartner partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    private MirrorProduct product;

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PIV));
        }
        recalculateAvailable();
    }

    @PreUpdate
    public void preUpdate() {
        recalculateAvailable();
    }

    public void recalculateAvailable() {
        this.quantityAvailable = Math.max(0, this.quantityOnHand - this.quantityReserved);
    }

    public boolean isLowStock() {
        return this.quantityAvailable <= this.reorderLevel;
    }

    public BigDecimal getEffectiveRetailPrice() {
        return partnerRetailPrice != null ? partnerRetailPrice : mirrorRetailPrice;
    }

    public BigDecimal getProfitPerUnit() {
        BigDecimal retail = getEffectiveRetailPrice();
        if (retail == null || wholesalePrice == null) return BigDecimal.ZERO;
        return retail.subtract(wholesalePrice);
    }

    public BigDecimal getMarginPercent() {
        BigDecimal retail = getEffectiveRetailPrice();
        if (retail == null || wholesalePrice == null || retail.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getProfitPerUnit()
            .multiply(BigDecimal.valueOf(100))
            .divide(retail, 2, RoundingMode.HALF_UP);
    }
}
