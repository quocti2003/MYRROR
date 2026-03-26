package com.mirror.product.dto.label;

import com.mirror.product.enums.LabelType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating or updating a Label Template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(max = 100, message = "Template name must not exceed 100 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "ZPL content is required")
    private String zplContent;

    @NotNull(message = "Label type is required")
    private LabelType labelType;

    @Min(value = 1, message = "Width must be positive")
    private Integer widthMm;

    @Min(value = 1, message = "Height must be positive")
    private Integer heightMm;

    @Min(value = 1, message = "DPI must be positive")
    @Builder.Default
    private Integer dpi = 203;

    /**
     * List of variable names used in the ZPL template
     * e.g., ["SKU", "PRICE", "BARCODE"]
     */
    private List<String> variables;

    private String previewImageUrl;

    private Boolean isDefault;
}
