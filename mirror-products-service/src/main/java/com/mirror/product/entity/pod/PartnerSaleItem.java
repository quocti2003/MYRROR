package com.mirror.product.entity.pod;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Entity representing a line item in a partner sale.
 * Tracks per-item cost, selling price, and profit.
 */
@Entity
@Table(name = "partner_sale_items", indexes = {
    @Index(name = "idx_sale_items_sale", columnList = "sale_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerSaleItem extends BaseEntity {

    @Column(name = "sale_id", nullable = false)
    private String saleId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "inventory_id", nullable = false)
    private String inventoryId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "wholesale_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal wholesaleCost;

    @Column(name = "selling_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal profit;

    // === RELATIONSHIPS ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PartnerSale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    private MirrorProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PartnerInventory inventory;

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PSI));
        }
        calculateProfit();
    }

    @PreUpdate
    public void preUpdate() {
        calculateProfit();
    }

    public void calculateProfit() {
        if (sellingPrice == null || quantity == null || wholesaleCost == null) {
            this.lineTotal = BigDecimal.ZERO;
            this.profit = BigDecimal.ZERO;
            return;
        }
        this.lineTotal = sellingPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal totalCost = wholesaleCost.multiply(BigDecimal.valueOf(quantity));
        this.profit = lineTotal.subtract(totalCost);
    }
}
