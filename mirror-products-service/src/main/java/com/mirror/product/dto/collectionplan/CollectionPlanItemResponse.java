package com.mirror.product.dto.collectionplan;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollectionPlanItemResponse {

    private UUID id;
    private String productName;
    private String productType;
    private String baseDesign;
    private Integer targetQuantity;
    private BigDecimal estimatedUnitCost;
    private BigDecimal subtotal;
    private String jewelrySpecId;
}
