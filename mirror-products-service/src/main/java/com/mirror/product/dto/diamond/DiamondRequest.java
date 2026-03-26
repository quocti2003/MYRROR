package com.mirror.product.dto.diamond;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating/updating lab-grown diamonds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiamondRequest {

    // Diamond Specifications
    @NotBlank(message = "Color is required")
    @Size(min = 1, max = 2, message = "Color must be 1-2 characters (D, E, F, etc.)")
    private String color;

    @NotBlank(message = "Clarity is required")
    @Size(max = 10, message = "Clarity must be max 10 characters")
    private String clarity;

    @NotNull(message = "Carat weight is required")
    @DecimalMin(value = "0.01", message = "Carat weight must be at least 0.01")
    @DecimalMax(value = "99.99", message = "Carat weight must be less than 100")
    private BigDecimal caratWeight;

    @NotBlank(message = "Shape is required")
    @Size(max = 20, message = "Shape must be max 20 characters")
    private String shape;

    // Origin & Manufacturer
    @Size(max = 2, message = "Origin country must be 2 characters (ISO code)")
    private String originCountry;

    @Size(max = 10, message = "Manufacturer code must be max 10 characters")
    private String manufacturerCode;

    @Size(max = 200, message = "Manufacturer name must be max 200 characters")
    private String manufacturerName;

    @Size(max = 50, message = "Manufacturer serial must be max 50 characters")
    private String manufacturerSerial;

    // Certification
    @NotBlank(message = "Certificate number is required")
    @Size(max = 20, message = "Certificate number must be max 20 characters")
    private String certNumber;

    @Size(max = 20, message = "Certificate lab must be max 20 characters")
    private String certLab;

    @Size(max = 500, message = "Certificate URL must be max 500 characters")
    private String certUrl;

    // Pricing
    @DecimalMin(value = "0.00", message = "Unit price must be positive")
    private BigDecimal unitPriceUsd;

    @DecimalMin(value = "0.00", message = "Total price must be positive")
    private BigDecimal totalPriceUsd;

    // Import Information
    @Size(max = 50, message = "Invoice number must be max 50 characters")
    private String invoiceNumber;

    private LocalDate invoiceDate;

    @Size(max = 50, message = "Customs declaration must be max 50 characters")
    private String customsDeclaration;

    private LocalDate importDate;

    // Status
    @Size(max = 100, message = "Location must be max 100 characters")
    private String location;

    private String notes;
}
