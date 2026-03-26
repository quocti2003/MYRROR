package com.mirror.product.dto.label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for printing labels
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelPrintRequest {

    @NotBlank(message = "Template ID is required")
    private String templateId;

    /**
     * List of product IDs to print labels for
     * Each product gets its own label(s)
     */
    @NotEmpty(message = "At least one product ID is required")
    private List<String> productIds;

    /**
     * Number of copies per product
     */
    @Min(value = 1, message = "Quantity per product must be at least 1")
    @Builder.Default
    private Integer quantityPerProduct = 1;

    /**
     * Optional: Override variables for all labels
     */
    private Map<String, String> variableOverrides;

    /**
     * Printer endpoint URL (local print service)
     * e.g., "http://localhost:3001/print"
     */
    private String printerEndpoint;
}
