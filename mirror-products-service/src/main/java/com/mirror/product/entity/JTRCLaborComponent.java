package com.mirror.product.entity;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.JTRCLaborType;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Labor cost component specification for a JTRC
 *
 * Tracks various labor costs like setting, finishing, plating, etc.
 * Supports multiple labor components per JTRC
 */
@Entity
@Table(name = "jtrc_labor_components", indexes = {
    @Index(name = "idx_jtrc_labor_jtrc_id", columnList = "jtrc_id"),
    @Index(name = "idx_jtrc_labor_type", columnList = "labor_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JTRCLaborComponent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jtrc_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private JewelryTechnicalReport jtrc;

    @Enumerated(EnumType.STRING)
    @Column(name = "labor_type", nullable = false)
    private JTRCLaborType laborType; // SETTING, FINISHING, PLATING, OTHER

    @Column
    private String description; // Additional description of the labor

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal cost;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.JLC));
        }
    }
}
