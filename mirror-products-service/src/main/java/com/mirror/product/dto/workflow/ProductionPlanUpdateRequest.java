package com.mirror.product.dto.workflow;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for updating a Production Plan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanUpdateRequest {

    @Size(max = 255, message = "Plan name must not exceed 255 characters")
    private String name;

    private String workflowTemplateId; // Can change template if still in DRAFT/PLANNING

    private LocalDate targetStartDate;
    private LocalDate targetEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;
}
