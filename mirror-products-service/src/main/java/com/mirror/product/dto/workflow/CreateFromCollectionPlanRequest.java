package com.mirror.product.dto.workflow;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a Production Plan from a Collection Plan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFromCollectionPlanRequest {

    @NotNull(message = "Collection Plan ID is required")
    private UUID collectionPlanId;

    @NotBlank(message = "Workflow template ID is required")
    private String workflowTemplateId;

    @Size(max = 255, message = "Plan name must not exceed 255 characters")
    private String name; // Optional: if not provided, will be generated from collection plan

    private LocalDate targetStartDate;
    private LocalDate targetEndDate;

    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;
}
