package com.mirror.product.entity;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.StoneColorCategory;
import com.mirror.product.enums.StoneRole;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Stone component specification for a JTRC
 *
 * Supports multiple stones per JTRC with different roles (Main, Side, Melee)
 * Handles different color categories with appropriate grading fields
 */
@Entity
@Table(name = "jtrc_stone_components", indexes = {
    @Index(name = "idx_jtrc_stone_jtrc_id", columnList = "jtrc_id"),
    @Index(name = "idx_jtrc_stone_role", columnList = "stone_role")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JTRCStoneComponent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jtrc_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private JewelryTechnicalReport jtrc;

    @Enumerated(EnumType.STRING)
    @Column(name = "stone_role", nullable = false)
    private StoneRole stoneRole; // MAIN, MAIN_2, SIDE, SIDE_2, MELEE

    @Column(name = "stone_type", nullable = false)
    private String stoneType; // "Lab Diamond", "Natural Diamond", "Ruby", "Sapphire", etc.

    @Column(name = "stone_detail")
    private String stoneDetail; // Specific stone name/details (e.g., "CVD Hearts & Arrows")

    @Column(nullable = false)
    private String shape; // "Round Brilliant", "Princess", "Emerald", "Cushion", etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "color_category", nullable = false)
    private StoneColorCategory colorCategory; // COLORLESS, FANCY_COLOR, COLOR_STONE

    // === Colorless Diamond Grading (D-J) ===
    @Column(name = "color_grade")
    private String colorGrade; // D, E, F, G, H, I, J (only for COLORLESS)

    // === Fancy Color Diamond Fields ===
    @Column(name = "color_intensity")
    private String colorIntensity; // "Fancy Light", "Fancy", "Fancy Intense", "Fancy Vivid"

    @Column(name = "color_name")
    private String colorName; // "Yellow", "Pink", "Blue", "Green", etc.

    // === Common Stone Properties ===
    @Column
    private String clarity; // FL, IF, VVS1, VVS2, VS1, VS2, SI1, SI2, I1

    @Column(name = "size_mm")
    private String sizeMm; // e.g., "6.5 mm", "6.5 x 4.5 mm"

    @Column(name = "weight_carat", precision = 10, scale = 3)
    private BigDecimal weightCarat;

    @Column(nullable = false)
    private Integer quantity = 1;

    // === Pricing ===
    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 15, scale = 2)
    private BigDecimal totalPrice; // unitPrice * quantity

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.JSC));
        }
        if (quantity == null) {
            quantity = 1;
        }
    }

    /**
     * Calculate total price based on unit price and quantity
     */
    public void calculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /**
     * Validate color fields based on color category
     */
    public boolean isValidColorConfiguration() {
        switch (colorCategory) {
            case COLORLESS:
                return colorGrade != null && !colorGrade.isEmpty();
            case FANCY_COLOR:
                return colorIntensity != null && colorName != null;
            case COLOR_STONE:
                return colorName != null && !colorName.isEmpty();
            default:
                return false;
        }
    }
}
