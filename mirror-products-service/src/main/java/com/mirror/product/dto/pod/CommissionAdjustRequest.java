package com.mirror.product.dto.pod;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request to add an adjustment to a commission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionAdjustRequest {

    @NotNull(message = "Adjustment amount is required")
    private BigDecimal amount;

    /**
     * Reason for the adjustment (required for audit trail).
     */
    @NotNull(message = "Adjustment reason is required")
    private String reason;
}
