package com.mirror.product.dto.productionorder;

import com.mirror.product.enums.ProductionOrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating Production Order status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOrderStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ProductionOrderStatus status;

    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;
}
