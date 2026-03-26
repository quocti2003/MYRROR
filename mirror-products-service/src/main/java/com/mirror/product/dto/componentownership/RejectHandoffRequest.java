package com.mirror.product.dto.componentownership;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for rejecting a component handoff
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectHandoffRequest {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;

    /**
     * Additional notes about the rejection
     */
    private String notes;
}
