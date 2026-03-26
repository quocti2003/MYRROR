package com.mirror.product.dto.productionorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for bulk assigning vendors to multiple stages
 * Used by P3-03: Bulk Partner Assignment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAssignRequest {

    @NotNull(message = "Assignments list is required")
    @NotEmpty(message = "At least one assignment is required")
    @Valid
    private List<StageAssignment> assignments;

    /**
     * Individual stage assignment
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageAssignment {

        @NotNull(message = "Stage ID is required")
        private String stageId;

        @NotNull(message = "Vendor ID is required")
        private String vendorId;

        private java.time.LocalDate plannedStartDate;
        private java.time.LocalDate plannedEndDate;
        private java.math.BigDecimal estimatedCost;
        private String notes;
    }
}
