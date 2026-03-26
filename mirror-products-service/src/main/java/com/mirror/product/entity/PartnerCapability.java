package com.mirror.product.entity;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.PartnerCapabilityType;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Partner Capability Entity
 *
 * Defines what production capabilities a vendor/partner has.
 * Enables flexible role assignment in production workflows.
 * A vendor can have multiple capabilities with different rates/lead times.
 */
@Entity
@Table(name = "partner_capabilities", indexes = {
    @Index(name = "idx_partner_cap_vendor", columnList = "vendor_id"),
    @Index(name = "idx_partner_cap_type", columnList = "capability_type")
}, uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_vendor_capability",
        columnNames = {"vendor_id", "capability_type"}
    )
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PartnerCapability extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    @ToString.Exclude
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "capability_type", nullable = false)
    private PartnerCapabilityType capabilityType;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays; // Expected days to complete this capability

    @Column(name = "cost_per_piece", precision = 15, scale = 2)
    private BigDecimal costPerPiece; // Cost charged per piece

    @Column(name = "cost_per_gram", precision = 15, scale = 2)
    private BigDecimal costPerGram; // Cost charged per gram (for metal work)

    @Column(name = "quality_rating")
    private Integer qualityRating; // 1-5 quality rating

    @Column(columnDefinition = "TEXT")
    private String notes; // Additional capability notes

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PCP));
        }
    }
}
