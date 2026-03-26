package com.mirror.product.dto.workflow;

import com.mirror.product.enums.WorkflowTemplateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search criteria for Workflow Templates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTemplateSearchCriteria {

    private String search; // Text search across name, description, category
    private String category;
    private WorkflowTemplateStatus status;
    private Boolean isDefault;
}
