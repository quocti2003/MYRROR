package com.mirror.product.dto.workflow;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new Production Plan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanCreateRequest {

    @NotBlank(message = "Plan name is required")
    @Size(max = 255, message = "Plan name must not exceed 255 characters")
    private String name;

    private UUID collectionPlanId; // Optional link to collection plan

    @NotBlank(message = "Workflow template ID is required")
    private String workflowTemplateId;

    private LocalDate targetStartDate;
    private LocalDate targetEndDate;

    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;
}
