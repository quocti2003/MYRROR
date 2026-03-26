package com.mirror.product.dto.workflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.PartnerCapabilityType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Workflow Stage
 * Used for both create/update requests and responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowStageDTO {

    private String id;

    @NotBlank(message = "Stage name is required")
    @Size(max = 255, message = "Stage name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Stage order is required")
    @Min(value = 1, message = "Stage order must be at least 1")
    private Integer stageOrder;

    private PartnerCapabilityType requiredCapability;

    @Min(value = 0, message = "Estimated duration must be non-negative")
    private Integer estimatedDurationDays;

    @Size(max = 5000, message = "Instructions must not exceed 5000 characters")
    private String instructions;

    private Boolean isFinalStage;
}
