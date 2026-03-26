package com.mirror.product.dto.pod;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodProductAssignRequest {

    @NotEmpty(message = "At least one product ID is required")
    private Set<String> productIds;

    /**
     * If true, replaces all existing products. If false, adds to existing.
     */
    private boolean replaceExisting = true;
}
