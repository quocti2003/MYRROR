package com.mirror.product.dto.jtrc;

import com.mirror.product.enums.StoneColorCategory;
import com.mirror.product.enums.StoneRole;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for JTRC Stone Component
 * Used for both request and response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JTRCStoneComponentDTO {

    private String id;

    @NotNull(message = "Stone role is required")
    private StoneRole stoneRole;

    @NotBlank(message = "Stone type is required")
    @Size(max = 100, message = "Stone type must not exceed 100 characters")
    private String stoneType;

    @Size(max = 200, message = "Stone detail must not exceed 200 characters")
    private String stoneDetail;

    @NotBlank(message = "Shape is required")
    @Size(max = 100, message = "Shape must not exceed 100 characters")
    private String shape;

    @NotNull(message = "Color category is required")
    private StoneColorCategory colorCategory;

    // Colorless diamond grading (D-J)
    @Size(max = 10, message = "Color grade must not exceed 10 characters")
    private String colorGrade;

    // Fancy color diamond fields
    @Size(max = 50, message = "Color intensity must not exceed 50 characters")
    private String colorIntensity;

    @Size(max = 50, message = "Color name must not exceed 50 characters")
    private String colorName;

    // Common stone properties
    @Size(max = 20, message = "Clarity must not exceed 20 characters")
    private String clarity;

    @Size(max = 50, message = "Size in mm must not exceed 50 characters")
    private String sizeMm;

    @DecimalMin(value = "0.001", message = "Weight in carat must be greater than 0")
    @Digits(integer = 7, fraction = 3, message = "Weight carat must have at most 7 integer digits and 3 decimal places")
    private BigDecimal weightCarat;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @DecimalMin(value = "0.0", message = "Unit price must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Unit price must have at most 13 integer digits and 2 decimal places")
    private BigDecimal unitPrice;

    // Calculated field (read-only in responses)
    private BigDecimal totalPrice;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
