package com.mirror.product.dto.sourcing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Sourcing Labor Specification DTO
 *
 * Labor specifications extracted from JTRC for sourcing report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourcingLaborSpec {

    private String laborType;        // SETTING, FINISHING, POLISHING, PLATING, etc.
    private String description;      // Additional description
    private BigDecimal unitCost;     // Cost per unit
    private Integer quantity;        // Number of units
    private BigDecimal totalCost;    // unitCost * quantity
}
