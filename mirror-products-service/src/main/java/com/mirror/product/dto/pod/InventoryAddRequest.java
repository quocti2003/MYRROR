package com.mirror.product.dto.pod;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAddRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotNull(message = "Initial quantity is required")
    @Min(value = 0, message = "Initial quantity must be non-negative")
    @Max(value = 100000, message = "Initial quantity must not exceed 100,000")
    private Integer initialQuantity;

    @DecimalMin(value = "0.0", message = "Partner retail price must be non-negative")
    private BigDecimal partnerRetailPrice;

    @Min(value = 1, message = "Reorder level must be at least 1")
    private Integer reorderLevel;

    @Min(value = 1, message = "Max stock level must be at least 1")
    private Integer maxStockLevel;
}
