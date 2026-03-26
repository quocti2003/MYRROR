package com.mirror.product.dto.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new Workflow Template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTemplateCreateRequest {

    @NotBlank(message = "Template name is required")
    @Size(max = 255, message = "Template name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category; // e.g., "RING", "NECKLACE", "EARRING"

    private Boolean isDefault; // Make this the default template for the category

    @Valid
    @Size(min = 1, message = "At least one stage is required")
    private List<WorkflowStageDTO> stages;
}
