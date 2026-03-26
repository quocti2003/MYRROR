package com.mirror.product.dto.label;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelTemplateUpdateRequest {

    private String name;

    private String description;

    private BigDecimal labelWidth;

    private BigDecimal labelHeight;

    private Integer dpi;

    private String canvasJson;

    private String previewImage; // Base64 encoded preview image

    private Boolean isDefault;

    private String status; // ACTIVE, INACTIVE
}
