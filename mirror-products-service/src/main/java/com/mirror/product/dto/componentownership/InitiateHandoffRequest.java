package com.mirror.product.dto.componentownership;

import com.mirror.product.enums.HandoffType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for initiating a component handoff
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateHandoffRequest {

    @NotBlank(message = "Production order ID is required")
    private String productionOrderId;

    /**
     * Optional stage ID (null for non-stage transfers like RETURN_TO_MIRROR)
     */
    private String stageId;

    /**
     * Vendor handing off the component (null for INITIAL_ASSIGNMENT from MIRROR)
     */
    private String fromVendorId;

    /**
     * Vendor receiving the component (null for RETURN_TO_MIRROR)
     */
    private String toVendorId;

    @NotNull(message = "Handoff type is required")
    private HandoffType handoffType;

    /**
     * Expected arrival date for planning
     */
    private LocalDate expectedArrivalDate;

    /**
     * Reason for the handoff
     */
    private String reason;

    /**
     * Additional notes
     */
    private String notes;
}
