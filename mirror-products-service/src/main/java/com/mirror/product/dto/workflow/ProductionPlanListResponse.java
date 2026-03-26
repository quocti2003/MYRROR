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
 * Response DTO for Production Plan in list views (summary)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductionPlanListResponse {

    private String id;
    private String name;
    private ProductionPlanStatus status;

    // References (IDs and names for display)
    private UUID collectionPlanId;
    private String collectionPlanName;
    private String workflowTemplateId;
    private String workflowTemplateName;

    // Key dates
    private LocalDate targetStartDate;
    private LocalDate targetEndDate;

    // Audit fields
    private Instant createdAt;
    private Instant updatedAt;
}
