package com.mirror.product.dto.productionorder;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for completing a stage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageCompleteRequest {

    private BigDecimal actualCost;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
