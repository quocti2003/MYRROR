package com.mirror.product.dto.productionorder;

import com.mirror.product.enums.ProductionOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search criteria for Production Orders
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOrderSearchCriteria {

    private String search; // Text search in order number and notes
    private String productionPlanId;
    private ProductionOrderStatus status;
    private String vendorId; // Filter by current holder
}
