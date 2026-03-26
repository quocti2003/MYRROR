package com.mirror.product.dto.workflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.WorkflowTemplateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for Workflow Template in list views (summary without stages)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowTemplateListResponse {

    private String id;
    private String name;
    private String description;
    private String category;
    private Boolean isDefault;
    private WorkflowTemplateStatus status;

    // Summary fields
    private Integer stageCount;
    private Integer totalEstimatedDurationDays;

    // Audit fields
    private Instant createdAt;
    private Instant updatedAt;
}
