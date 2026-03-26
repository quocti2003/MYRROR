package com.mirror.product.dto.collectionplan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionPlanItemRequest {

    @NotBlank(message = "Product name is required")
    private String productName;

    @NotBlank(message = "Product type is required")
    private String productType;

    private String baseDesign;

    @NotNull(message = "Target quantity is required")
    private Integer targetQuantity;

    private BigDecimal estimatedUnitCost;
}
