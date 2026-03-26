package com.mirror.product.entity.pod;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.enums.CommissionStatus;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a partner's commission for a period.
 * Aggregates attributed orders and calculates commission amounts.
 */
@Entity
@Table(name = "pod_commissions", indexes = {
    @Index(name = "idx_commission_partner", columnList = "partner_id"),
    @Index(name = "idx_commission_status", columnList = "status"),
    @Index(name = "idx_commission_period", columnList = "period_start, period_end")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_commission_partner_period", columnNames = {"partner_id", "period_start", "period_end"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PodCommission extends BaseEntity {

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PodPartner partner;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_orders", nullable = false)
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "total_order_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalOrderAmount = BigDecimal.ZERO;

    @Column(name = "attributed_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal attributedAmount = BigDecimal.ZERO;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "adjustments", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal adjustments = BigDecimal.ZERO;

    @Column(name = "adjustment_reason", columnDefinition = "TEXT")
    private String adjustmentReason;

    @Column(name = "final_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CommissionStatus status = CommissionStatus.PENDING;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "commission")
    @Builder.Default
    private List<PodAttribution> attributions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PCM));
        }
        calculateFinalAmount();
    }

    /**
     * Calculate commission amount based on attributed amount and rate
     */
    public void calculateCommission() {
        if (attributedAmount != null && commissionRate != null) {
            this.commissionAmount = attributedAmount.multiply(commissionRate).divide(new BigDecimal("100"));
            calculateFinalAmount();
        }
    }

    /**
     * Calculate final amount after adjustments
     */
    public void calculateFinalAmount() {
        if (commissionAmount != null) {
            BigDecimal adj = adjustments != null ? adjustments : BigDecimal.ZERO;
            this.finalAmount = commissionAmount.add(adj);
        }
    }

    /**
     * Approve the commission
     */
    public void approve(String approver) {
        this.status = CommissionStatus.APPROVED;
        this.approvedAt = Instant.now();
        this.approvedBy = approver;
    }

    /**
     * Mark as paid
     */
    public void markAsPaid(String reference, String method) {
        this.status = CommissionStatus.PAID;
        this.paidAt = Instant.now();
        this.paymentReference = reference;
        this.paymentMethod = method;
    }
}
