package com.mirror.product.dto.label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for rendering a label with variable substitution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelRenderRequest {

    @NotBlank(message = "Template ID is required")
    private String templateId;

    /**
     * Optional: provide a product ID to automatically populate variables
     * from the product data
     */
    private String productId;

    /**
     * Manual variable values for substitution
     * Key: variable name (e.g., "SKU", "PRICE")
     * Value: value to substitute
     */
    private Map<String, String> variables;

    /**
     * Number of copies to print
     */
    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;
}
