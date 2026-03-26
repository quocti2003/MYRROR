package com.mirror.product.dto.label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelTemplateCreateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;

    @NotNull(message = "Label width is required")
    @Positive(message = "Label width must be positive")
    private BigDecimal labelWidth;

    @NotNull(message = "Label height is required")
    @Positive(message = "Label height must be positive")
    private BigDecimal labelHeight;

    @Builder.Default
    private Integer dpi = 300;

    @NotBlank(message = "Canvas JSON is required")
    private String canvasJson;

    private String previewImage; // Base64 encoded preview image

    @Builder.Default
    private Boolean isDefault = false;
}
