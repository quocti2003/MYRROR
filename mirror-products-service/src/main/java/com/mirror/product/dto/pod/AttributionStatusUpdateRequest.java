package com.mirror.product.dto.pod;

import com.mirror.product.enums.AttributionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributionStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private AttributionStatus status;

    /**
     * Optional notes about the status change.
     */
    private String notes;
}
