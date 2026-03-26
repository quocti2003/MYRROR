package com.mirror.product.dto.workflow;

import com.mirror.product.enums.ProductionPlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Search criteria for Production Plans
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanSearchCriteria {

    private String search; // Text search across name, notes
    private ProductionPlanStatus status;
    private UUID collectionPlanId;
    private String workflowTemplateId;
}
