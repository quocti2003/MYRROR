package com.mirror.product.entity.pod;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.WholesaleOrderStatus;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import com.mirror.product.exception.pod.InvalidOrderStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a wholesale purchase order from a phygital partner.
 * Partners buy products at wholesale prices to resell at their own retail price.
 */
@Entity
@Table(name = "wholesale_orders", indexes = {
    @Index(name = "idx_wholesale_orders_partner", columnList = "partner_id"),
    @Index(name = "idx_wholesale_orders_status", columnList = "status"),
    @Index(name = "idx_wholesale_orders_created", columnList = "created_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WholesaleOrder extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WholesaleOrderStatus status = WholesaleOrderStatus.DRAFT;

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

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "shipping_method", length = 50)
    private String shippingMethod;

    @Column(name = "shipping_cost", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "UNPAID";

    @Column(columnDefinition = "TEXT")
    private String notes;

    // === RELATIONSHIPS ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PodPartner partner;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WholesaleOrderItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.WHO));
        }
        if (this.orderNumber == null) {
            this.orderNumber = "WO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
            .map(WholesaleOrderItem::getLineTotal)
            .filter(lt -> lt != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = subtotal
            .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
            .add(taxAmount != null ? taxAmount : BigDecimal.ZERO)
            .add(shippingCost != null ? shippingCost : BigDecimal.ZERO);
    }

    public void submit() {
        if (this.status != WholesaleOrderStatus.DRAFT) {
            throw new InvalidOrderStateException("Cannot submit order in status: " + this.status);
        }
        this.status = WholesaleOrderStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public void approve(String approver) {
        if (this.status != WholesaleOrderStatus.SUBMITTED) {
            throw new InvalidOrderStateException("Cannot approve order in status: " + this.status);
        }
        this.status = WholesaleOrderStatus.APPROVED;
        this.approvedAt = Instant.now();
        this.approvedBy = approver;
    }

    public void cancel(String cancelledBy, String reason) {
        if (this.status == WholesaleOrderStatus.SHIPPED ||
            this.status == WholesaleOrderStatus.DELIVERED ||
            this.status == WholesaleOrderStatus.COMPLETED) {
            throw new InvalidOrderStateException("Cannot cancel order in status: " + this.status);
        }
        this.status = WholesaleOrderStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = cancelledBy;
        this.cancellationReason = reason;
    }
}
