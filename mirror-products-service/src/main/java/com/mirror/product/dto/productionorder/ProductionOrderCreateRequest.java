package com.mirror.product.dto.productionorder;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a single Production Order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductionOrderCreateRequest {

    @NotBlank(message = "Production plan ID is required")
    private String productionPlanId;

    private UUID collectionPlanItemId;

    private String jtrcId;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    private LocalDate estimatedCompletionDate;

    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;
}
