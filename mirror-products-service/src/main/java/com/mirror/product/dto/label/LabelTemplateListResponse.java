package com.mirror.product.dto.label;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.LabelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Lightweight response DTO for Label Template listings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LabelTemplateListResponse {

    private String id;
    private String name;
    private String description;
    private LabelType labelType;
    private Integer widthMm;
    private Integer heightMm;
    private Integer dpi;
    private List<String> variables;
    private String previewImageUrl;
    private Boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}
