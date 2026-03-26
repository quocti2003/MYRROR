package com.mirror.product.dto.collectionplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionPlanUpdateRequest {

    private String name;

    private Integer totalQuantity;

    private LocalDate deadline;

    private LocalDate estimatedDeliveryDate;

    private List<CollectionPlanItemRequest> items;
}
