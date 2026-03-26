package com.mirror.product.dto.productionorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for assigning a vendor to a stage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageAssignVendorRequest {

    @NotBlank(message = "Vendor ID is required")
    private String vendorId;

    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private BigDecimal estimatedCost;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
