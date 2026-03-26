package com.mirror.product.dto.workflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.ProductionPlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for Production Plan with full details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductionPlanResponse {

    private String id;
    private String name;
    private ProductionPlanStatus status;

    // Collection Plan reference
    private UUID collectionPlanId;
    private String collectionPlanName; // Included for display

    // Workflow Template reference
    private String workflowTemplateId;
    private String workflowTemplateName; // Included for display
    private WorkflowTemplateResponse workflowTemplate; // Optional: full template details

    // Dates
    private LocalDate targetStartDate;
    private LocalDate targetEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    private String notes;

    // Computed fields
    private Boolean isEditable;
    private Boolean isCancellable;

    // Audit fields
    private String createdBy;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isActive;
}
