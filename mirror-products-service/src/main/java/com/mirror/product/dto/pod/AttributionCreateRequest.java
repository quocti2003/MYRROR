package com.mirror.product.dto.pod;

import com.mirror.product.enums.AttributionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request to manually create an attribution (admin use).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributionCreateRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "POD ID is required")
    private String podId;

    /**
     * Optional scan ID to link to a specific scan.
     */
    private String scanId;

    /**
     * Attribution type (FIRST_TOUCH, LAST_TOUCH, MULTI_TOUCH).
     */
    @Builder.Default
    private AttributionType attributionType = AttributionType.LAST_TOUCH;

    /**
     * Attribution weight for multi-touch (0.0 to 1.0).
     */
    @Builder.Default
    private BigDecimal attributionWeight = BigDecimal.ONE;

    /**
     * Optional notes about the attribution.
     */
    private String notes;
}
