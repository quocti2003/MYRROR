package com.mirror.product.dto.jtrc;

import com.mirror.product.enums.JTRCLaborType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for JTRC Labor Component
 * Used for both request and response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JTRCLaborComponentDTO {

    private String id;

    @NotNull(message = "Labor type is required")
    private JTRCLaborType laborType;

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @NotNull(message = "Cost is required")
    @DecimalMin(value = "0.0", message = "Cost must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Cost must have at most 13 integer digits and 2 decimal places")
    private BigDecimal cost;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
