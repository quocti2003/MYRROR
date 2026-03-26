package com.mirror.product.dto.pod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryPriceUpdateRequest {

    @NotNull(message = "Partner retail price is required")
    @DecimalMin(value = "0.01", message = "Partner retail price must be greater than 0")
    private BigDecimal partnerRetailPrice;
}
