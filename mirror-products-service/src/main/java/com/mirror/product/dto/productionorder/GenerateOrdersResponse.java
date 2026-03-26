package com.mirror.product.dto.productionorder;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for the generate orders operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerateOrdersResponse {

    private String productionPlanId;
    private String productionPlanName;
    private Integer ordersGenerated;
    private Integer stagesCreated;
    private List<ProductionOrderListResponse> orders;
    private String message;
}
