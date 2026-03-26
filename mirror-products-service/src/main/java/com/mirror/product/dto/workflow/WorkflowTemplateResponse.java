package com.mirror.product.dto.workflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.WorkflowTemplateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for Workflow Template with full details including stages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowTemplateResponse {

    private String id;
    private String name;
    private String description;
    private String category;
    private Boolean isDefault;
    private WorkflowTemplateStatus status;

    private List<WorkflowStageDTO> stages;

    // Computed fields
    private Integer stageCount;
    private Integer totalEstimatedDurationDays;

    // Audit fields
    private String createdBy;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isActive;
}
