package com.mirror.product.dto.diamond;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for generating lab-grown diamond SKU codes
 * Format: LGD-{COLOR}{CLARITY}-{CARAT}-{SHAPE}-{ORIGIN}-{MFR}-{CERT}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiamondSkuRequest {

    @NotBlank(message = "Color is required")
    @Size(min = 1, max = 2, message = "Color must be 1-2 characters (D, E, F, etc.)")
    private String color;

    @NotBlank(message = "Clarity is required")
    @Size(max = 10, message = "Clarity must be max 10 characters (VVS1, VS1, etc.)")
    private String clarity;

    @NotNull(message = "Carat weight is required")
    @DecimalMin(value = "0.01", message = "Carat weight must be at least 0.01")
    @DecimalMax(value = "99.99", message = "Carat weight must be less than 100")
    private BigDecimal caratWeight;

    @NotBlank(message = "Shape is required")
    @Size(max = 2, message = "Shape code must be 2 characters (RD, PR, CU, etc.)")
    private String shape; // RD, PR, CU, etc.

    @NotBlank(message = "Origin country is required")
    @Size(min = 2, max = 2, message = "Origin must be 2-character country code (IN, CN, US, etc.)")
    private String origin;

    @NotBlank(message = "Manufacturer code is required")
    @Size(max = 10, message = "Manufacturer code must be max 10 characters")
    private String manufacturerCode; // KARP, SOLI, NDT, etc.

    @NotBlank(message = "Certificate number is required")
    @Size(max = 20, message = "Certificate number must be max 20 characters")
    private String certNumber;
}
