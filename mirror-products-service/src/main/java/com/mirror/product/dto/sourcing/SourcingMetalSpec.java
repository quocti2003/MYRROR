package com.mirror.product.dto.sourcing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Sourcing Metal Specification DTO
 *
 * Metal specifications extracted from JTRC for sourcing report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourcingMetalSpec {

    private String metalType;        // GOLD, PLATINUM, SILVER, etc.
    private String purity;           // 18K, 14K, etc.
    private String color;            // YELLOW, WHITE, ROSE, etc.
    private BigDecimal grossWeight;  // grams
    private BigDecimal netWeight;    // grams (after stone deductions)
    private BigDecimal lossRate;     // percentage for manufacturing loss
    private BigDecimal totalWeight;  // grossWeight * (1 + lossRate)
    private BigDecimal estimatedCost; // in VND
}
