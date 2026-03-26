package com.mirror.product.entity;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Metal component specification for a JTRC
 *
 * Contains metal type, purity, weight, loss calculations, and costs
 * One-to-one relationship with JewelryTechnicalReport
 */
@Entity
@Table(name = "jtrc_metal_components")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JTRCMetalComponent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jtrc_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private JewelryTechnicalReport jtrc;

    @Column(name = "metal_type", nullable = false)
    private String metalType; // e.g., "Yellow Gold", "White Gold", "Rose Gold", "Platinum"

    @Column(name = "metal_purity", nullable = false)
    private String metalPurity; // e.g., "18K", "14K", "10K", "Sterling Silver"

    @Column(name = "weight_grams", precision = 10, scale = 3, nullable = false)
    private BigDecimal weightGrams;

    @Column(name = "loss_rate_percent", precision = 5, scale = 2)
    private BigDecimal lossRatePercent; // Typical 1-6% loss during production

    @Column(name = "price_per_gram", precision = 15, scale = 2)
    private BigDecimal pricePerGram; // Price at time of quote

    // === Calculated Fields ===
    @Column(name = "metal_cost", precision = 15, scale = 2)
    private BigDecimal metalCost; // weight * pricePerGram

    @Column(name = "loss_cost", precision = 15, scale = 2)
    private BigDecimal lossCost; // metalCost * lossRatePercent

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost; // metalCost + lossCost

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.JMC));
        }
    }

    /**
     * Calculate costs based on weight, price, and loss rate
     * Called by service layer when saving
     */
    public void calculateCosts() {
        if (weightGrams != null && pricePerGram != null) {
            this.metalCost = weightGrams.multiply(pricePerGram);

            if (lossRatePercent != null) {
                this.lossCost = metalCost.multiply(lossRatePercent)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            } else {
                this.lossCost = BigDecimal.ZERO;
            }

            this.totalCost = metalCost.add(lossCost != null ? lossCost : BigDecimal.ZERO);
        }
    }
}
