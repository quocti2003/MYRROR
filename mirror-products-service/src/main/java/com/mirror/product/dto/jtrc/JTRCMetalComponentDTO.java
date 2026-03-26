package com.mirror.product.dto.jtrc;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for JTRC Metal Component
 * Used for both request and response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JTRCMetalComponentDTO {

    private String id;

    @NotBlank(message = "Metal type is required")
    @Size(max = 100, message = "Metal type must not exceed 100 characters")
    private String metalType;

    @NotBlank(message = "Metal purity is required")
    @Size(max = 50, message = "Metal purity must not exceed 50 characters")
    private String metalPurity;

    @NotNull(message = "Weight in grams is required")
    @DecimalMin(value = "0.001", message = "Weight must be greater than 0")
    @Digits(integer = 7, fraction = 3, message = "Weight must have at most 7 integer digits and 3 decimal places")
    private BigDecimal weightGrams;

    @DecimalMin(value = "0.0", message = "Loss rate percent must be non-negative")
    @DecimalMax(value = "100.0", message = "Loss rate percent must not exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Loss rate must have at most 3 integer digits and 2 decimal places")
    private BigDecimal lossRatePercent;

    @DecimalMin(value = "0.0", message = "Price per gram must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Price per gram must have at most 13 integer digits and 2 decimal places")
    private BigDecimal pricePerGram;

    // Calculated fields (read-only in responses)
    private BigDecimal metalCost;
    private BigDecimal lossCost;
    private BigDecimal totalCost;
}
