package com.mirror.product.dto.label;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.entity.label.LabelTemplate;
import com.mirror.product.enums.LabelTemplateStatus;
import com.mirror.product.enums.LabelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for Label Template with full details
 * Supports both RFID label system (canvasJson) and ZPL-based label system (zplContent)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LabelTemplateResponse {

    private String id;
    private String name;
    private String description;

    // Dimensions - support both formats
    private BigDecimal labelWidth;
    private BigDecimal labelHeight;
    private Integer widthMm;
    private Integer heightMm;
    private Integer dpi;

    // RFID Label System fields
    private String canvasJson;
    private String previewUrl;
    private LabelTemplateStatus status;
    private String createdBy;
    private String updatedBy;

    // ZPL-based Label System fields
    private String zplContent;
    private LabelType labelType;
    private List<String> variables;
    private String previewImageUrl;
    private Boolean isDefault;
    private Boolean isActive;

    private Instant createdAt;
    private Instant updatedAt;

    public static LabelTemplateResponse fromEntity(LabelTemplate entity) {
        if (entity == null) return null;

        return LabelTemplateResponse.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .labelWidth(entity.getLabelWidth())
            .labelHeight(entity.getLabelHeight())
            .dpi(entity.getDpi())
            .canvasJson(entity.getCanvasJson())
            .previewUrl(entity.getPreviewUrl())
            .isDefault(entity.getIsDefault())
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
