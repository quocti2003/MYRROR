package com.mirror.product.dto.pod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request to generate commission for a partner for a specific period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionGenerateRequest {

    @NotBlank(message = "Partner ID is required")
    private String partnerId;

    @NotNull(message = "Period start is required")
    private LocalDate periodStart;

    @NotNull(message = "Period end is required")
    private LocalDate periodEnd;

    /**
     * If true, regenerate commission even if one already exists for this period.
     */
    @Builder.Default
    private boolean regenerate = false;
}
