package com.mirror.product.entity.pod;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.PartnerSaleStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a sale made by a phygital partner to a customer.
 * Tracks revenue, cost of goods, and profit.
 */
@Entity
@Table(name = "partner_sales", indexes = {
    @Index(name = "idx_partner_sales_partner_date", columnList = "partner_id,sold_at"),
    @Index(name = "idx_partner_sales_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerSale extends BaseEntity {

    @Column(name = "sale_number", nullable = false, unique = true, length = 30)
    private String saleNumber;

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @Column(name = "pod_id")
    private String podId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PartnerSaleStatus status = PartnerSaleStatus.PENDING;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "cost_of_goods", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal costOfGoods = BigDecimal.ZERO;

    @Column(name = "profit_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal profitAmount = BigDecimal.ZERO;

    @Column(name = "profit_margin_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal profitMarginPercent = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "qr_code_id")
    private String qrCodeId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "sold_at", nullable = false)
    private Instant soldAt;

    // === RELATIONSHIPS ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PodPartner partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pod_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Pod pod;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PartnerSaleItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PSL));
        }
        if (this.saleNumber == null) {
            this.saleNumber = "SL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (this.soldAt == null) {
            this.soldAt = Instant.now();
        }
        calculateTotals();
    }

    @PreUpdate
    public void preUpdate() {
        calculateTotals();
    }

    public void calculateTotals() {
        if (items == null || items.isEmpty()) return;
        this.subtotal = items.stream()
            .map(PartnerSaleItem::getLineTotal)
            .filter(lt -> lt != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.costOfGoods = items.stream()
            .filter(i -> i.getWholesaleCost() != null && i.getQuantity() != null)
            .map(i -> i.getWholesaleCost().multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = subtotal
            .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
            .add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
        this.profitAmount = totalAmount.subtract(costOfGoods);
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.profitMarginPercent = profitAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(totalAmount, 2, RoundingMode.HALF_UP);
        }
    }
}
