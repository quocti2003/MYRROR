package com.mirror.product.dto.productops;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepUpdateRequest {
    @Min(1)
    @Max(7)
    private Integer stepId;

    @NotBlank
    private String status; // complete, pending, blocked

    private String notes;
    private String blockReason;
}
