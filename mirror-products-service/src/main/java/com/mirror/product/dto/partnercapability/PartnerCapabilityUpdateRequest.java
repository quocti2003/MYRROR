package com.mirror.product.dto.partnercapability;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating an existing Partner Capability
 * Note: capabilityType cannot be changed after creation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerCapabilityUpdateRequest {

    @Min(value = 0, message = "Lead time days must be non-negative")
    private Integer leadTimeDays;

    @DecimalMin(value = "0.0", message = "Cost per piece must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Cost per piece must have at most 13 integer digits and 2 decimal places")
    private BigDecimal costPerPiece;

    @DecimalMin(value = "0.0", message = "Cost per gram must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Cost per gram must have at most 13 integer digits and 2 decimal places")
    private BigDecimal costPerGram;

    @Min(value = 1, message = "Quality rating must be at least 1")
    @Max(value = 5, message = "Quality rating must not exceed 5")
    private Integer qualityRating;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
