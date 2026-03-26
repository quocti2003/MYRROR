package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DesignerRequest {

    @NotBlank(message = "Designer code is required")
    @Size(max = 50, message = "Designer code cannot exceed 50 characters")
    private String code;

    @NotBlank(message = "Designer name is required")
    @Size(max = 200, message = "Designer name cannot exceed 200 characters")
    private String name;

    @Size(max = 200, message = "Brand name cannot exceed 200 characters")
    private String brandName;

    @Size(max = 200, message = "Specialty cannot exceed 200 characters")
    private String specialty;

    @Min(value = 0, message = "Years of experience must be non-negative")
    private Integer yearsExperience;

    @Size(max = 100, message = "Design style cannot exceed 100 characters")
    private String designStyle;

    @DecimalMin(value = "0.00", message = "Commission percentage must be at least 0%")
    @DecimalMax(value = "100.00", message = "Commission percentage cannot exceed 100%")
    private BigDecimal defaultCommissionPercent;

    @DecimalMin(value = "0.00", message = "Loyalty percentage must be at least 0%")
    @DecimalMax(value = "100.00", message = "Loyalty percentage cannot exceed 100%")
    private BigDecimal defaultLoyaltyPercent;

    @Email(message = "Invalid email format")
    @Size(max = 200, message = "Contact email cannot exceed 200 characters")
    private String contactEmail;

    @Size(max = 50, message = "Contact phone cannot exceed 50 characters")
    private String contactPhone;

    @Size(max = 500, message = "Website URL cannot exceed 500 characters")
    private String website;

    private String socialMediaLinks;

    private String bio;

    @Size(max = 500, message = "Portfolio URL cannot exceed 500 characters")
    private String portfolioUrl;

    private Boolean verified;

    private Boolean featured;
}