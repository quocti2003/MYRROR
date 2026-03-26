package com.mirror.product.dto.workflow;

import com.mirror.product.enums.WorkflowTemplateStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating Workflow Template status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTemplateStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private WorkflowTemplateStatus status;

    private String reason; // Optional reason for status change
}
