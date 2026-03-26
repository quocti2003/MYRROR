package com.mirror.product.dto.sourcing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Sourcing Stone Specification DTO
 *
 * Stone specifications extracted from JTRC for sourcing report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourcingStoneSpec {

    private String stoneRole;        // MAIN, SIDE, MELEE, ACCENT
    private String stoneType;        // DIAMOND, RUBY, SAPPHIRE, EMERALD, etc.
    private String shape;            // ROUND, OVAL, PEAR, MARQUISE, etc.
    private String colorCategory;    // For colored stones
    private String colorGrade;       // For diamonds: D, E, F, etc.
    private String clarityGrade;     // VS1, VS2, SI1, etc.
    private String cutGrade;         // EXCELLENT, VERY_GOOD, etc.
    private BigDecimal caratWeight;  // carat per stone
    private Integer quantity;        // number of stones
    private BigDecimal totalCaratWeight; // caratWeight * quantity
    private String sizeRange;        // e.g., "1.5mm-2.0mm"
    private BigDecimal pricePerCarat;
    private BigDecimal estimatedCost; // in VND
}
