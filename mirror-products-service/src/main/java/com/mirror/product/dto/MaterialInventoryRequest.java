package com.mirror.product.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for Material Inventory - Raw materials and components for jewelry production
 * Migrated from mirror-mrp-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialInventoryRequest {

    @NotBlank(message = "Material name is required")
    @Size(max = 200, message = "Material name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Material type is required")
    @Size(max = 50, message = "Material type must not exceed 50 characters")
    private String type;

    @NotBlank(message = "Vendor ID is required")
    private String vendorId;

    @DecimalMin(value = "0.0", message = "Base price must be non-negative")
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0", message = "Market price must be non-negative")
    private BigDecimal marketPrice;

    @NotBlank(message = "Currency is required")
    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;

    @DecimalMin(value = "0.0", message = "Tax customs percent must be non-negative")
    @DecimalMax(value = "100.0", message = "Tax customs percent must not exceed 100")
    private BigDecimal taxCustomsPercent;

    @DecimalMin(value = "0.0", message = "Assembly cost must be non-negative")
    private BigDecimal assemblyCostLocal;

    @Min(value = 0, message = "Delivery lead time must be non-negative")
    private Integer deliveryLeadTimeDays;

    @Min(value = 0, message = "Production lead time must be non-negative")
    private Integer productionLeadTimeDays;

    @Min(value = 0, message = "Assembly lead time must be non-negative")
    private Integer assemblyLeadTimeDays;
}
