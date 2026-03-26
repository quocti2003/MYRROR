package com.mirror.product.dto.collectionplan;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionPlanStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;
}
