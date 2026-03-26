package com.mirror.product.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Age Group - Customer demographic segments
 * Migrated from mirror-mrp-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgeGroupRequest {

    @NotBlank(message = "Age group name is required")
    @Size(max = 100, message = "Age group name must not exceed 100 characters")
    private String name;

    @Min(value = 0, message = "Minimum age must be non-negative")
    @Max(value = 150, message = "Minimum age must not exceed 150")
    private Integer minAge;

    @Min(value = 0, message = "Maximum age must be non-negative")
    @Max(value = 150, message = "Maximum age must not exceed 150")
    private Integer maxAge;
}
