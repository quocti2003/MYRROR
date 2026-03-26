package com.mirror.product.dto.workflow;

import com.mirror.product.enums.ProductionPlanStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating Production Plan status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ProductionPlanStatus status;

    private String reason; // Optional reason for status change
}
