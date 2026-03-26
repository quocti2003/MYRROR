package com.mirror.product.entity.pod;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.entity.Order;
import com.mirror.product.enums.AttributionStatus;
import com.mirror.product.enums.AttributionType;
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
import java.time.temporal.ChronoUnit;

/**
 * Entity representing an order-to-POD attribution.
 * Links orders to the POD that contributed to the sale.
 */
@Entity
@Table(name = "pod_attributions", indexes = {
    @Index(name = "idx_attribution_order", columnList = "order_id"),
    @Index(name = "idx_attribution_pod", columnList = "pod_id"),
    @Index(name = "idx_attribution_partner", columnList = "partner_id"),
    @Index(name = "idx_attribution_status", columnList = "status"),
    @Index(name = "idx_attribution_order_placed", columnList = "order_placed_at"),
    @Index(name = "idx_attribution_partner_date", columnList = "partner_id, order_placed_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PodAttribution extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Order order;

    @Column(name = "pod_id", nullable = false)
    private String podId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pod_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Pod pod;

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PodPartner partner;

    @Column(name = "qr_code_id")
    private String qrCodeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_code_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PodQrCode qrCode;

    @Column(name = "scan_id")
    private String scanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PodQrScan scan;

    @Enumerated(EnumType.STRING)
    @Column(name = "attribution_type", nullable = false, length = 30)
    @Builder.Default
    private AttributionType attributionType = AttributionType.LAST_TOUCH;

    @Column(name = "attribution_weight", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal attributionWeight = BigDecimal.ONE;

    @Column(name = "order_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal orderAmount;

    @Column(name = "attributed_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal attributedAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "first_scan_at")
    private Instant firstScanAt;

    @Column(name = "last_scan_at")
    private Instant lastScanAt;

    @Column(name = "order_placed_at", nullable = false)
    private Instant orderPlacedAt;

    @Column(name = "days_to_conversion")
    private Integer daysToConversion;

    @Column(name = "touch_count")
    @Builder.Default
    private Integer touchCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AttributionStatus status = AttributionStatus.PENDING;

    @Column(name = "commission_id")
    private String commissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PodCommission commission;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PAT));
        }
        calculateDaysToConversion();
        calculateAttributedAmount();
    }

    /**
     * Calculate days between first scan and order placement
     */
    public void calculateDaysToConversion() {
        if (firstScanAt != null && orderPlacedAt != null) {
            this.daysToConversion = (int) ChronoUnit.DAYS.between(
                firstScanAt.truncatedTo(ChronoUnit.DAYS),
                orderPlacedAt.truncatedTo(ChronoUnit.DAYS)
            );
        }
    }

    /**
     * Calculate attributed amount based on order amount and weight
     */
    public void calculateAttributedAmount() {
        if (orderAmount != null && attributionWeight != null) {
            this.attributedAmount = orderAmount.multiply(attributionWeight);
        }
    }
}
