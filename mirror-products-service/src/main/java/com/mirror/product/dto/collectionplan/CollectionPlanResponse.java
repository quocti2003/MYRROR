package com.mirror.product.dto.collectionplan;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollectionPlanResponse {

    private UUID id;
    private String name;
    private Integer totalQuantity;
    private LocalDate deadline;
    private String status;
    private BigDecimal totalCost;
    private LocalDate estimatedDeliveryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<CollectionPlanItemResponse> items;
    private int itemCount;
    private boolean hasProductionPlans;
}
