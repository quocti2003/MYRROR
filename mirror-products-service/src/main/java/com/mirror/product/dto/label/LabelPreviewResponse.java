package com.mirror.product.dto.label;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for label preview generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LabelPreviewResponse {

    /**
     * The rendered ZPL code with all variables substituted
     */
    private String renderedZpl;

    /**
     * Base64-encoded PNG image of the label preview
     * Generated via Labelary API
     */
    private String previewImageBase64;

    /**
     * Direct URL to the preview image (if available)
     */
    private String previewImageUrl;

    /**
     * Label dimensions in mm
     */
    private Integer widthMm;
    private Integer heightMm;
    private Integer dpi;

    /**
     * Template info
     */
    private String templateId;
    private String templateName;
}
