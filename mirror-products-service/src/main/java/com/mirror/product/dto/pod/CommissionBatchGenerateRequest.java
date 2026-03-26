package com.mirror.product.dto.pod;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

/**
 * Request to generate commissions for multiple partners or all active partners.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionBatchGenerateRequest {

    /**
     * Optional list of partner IDs. If empty, generate for all active partners.
     */
    private Set<String> partnerIds;

    @NotNull(message = "Period start is required")
    private LocalDate periodStart;

    @NotNull(message = "Period end is required")
    private LocalDate periodEnd;

    /**
     * If true, regenerate commissions even if they already exist.
     */
    @Builder.Default
    private boolean regenerate = false;
}
