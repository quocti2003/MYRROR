package com.mirror.product.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.dto.label.LabelTemplateListResponse;
import com.mirror.product.dto.label.LabelTemplateRequest;
import com.mirror.product.dto.label.LabelTemplateResponse;
import com.mirror.product.entity.LabelTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for LabelTemplate entity and DTOs
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LabelTemplateMapper {

    private final ObjectMapper objectMapper;

    public LabelTemplateResponse toResponse(LabelTemplate entity) {
        if (entity == null) {
            return null;
        }
        return LabelTemplateResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .zplContent(entity.getZplContent())
                .labelType(entity.getLabelType())
                .widthMm(entity.getWidthMm())
                .heightMm(entity.getHeightMm())
                .dpi(entity.getDpi())
                .variables(parseVariables(entity.getVariables()))
                .previewImageUrl(entity.getPreviewImageUrl())
                .isDefault(entity.getIsDefault())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public LabelTemplateListResponse toListResponse(LabelTemplate entity) {
        if (entity == null) {
            return null;
        }
        return LabelTemplateListResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .labelType(entity.getLabelType())
                .widthMm(entity.getWidthMm())
                .heightMm(entity.getHeightMm())
                .dpi(entity.getDpi())
                .variables(parseVariables(entity.getVariables()))
                .previewImageUrl(entity.getPreviewImageUrl())
                .isDefault(entity.getIsDefault())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public LabelTemplate toEntity(LabelTemplateRequest request) {
        if (request == null) {
            return null;
        }
        return LabelTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .zplContent(request.getZplContent())
                .labelType(request.getLabelType())
                .widthMm(request.getWidthMm())
                .heightMm(request.getHeightMm())
                .dpi(request.getDpi() != null ? request.getDpi() : 203)
                .variables(serializeVariables(request.getVariables()))
                .previewImageUrl(request.getPreviewImageUrl())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();
    }

    public void updateEntity(LabelTemplate entity, LabelTemplateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getZplContent() != null) {
            entity.setZplContent(request.getZplContent());
        }
        if (request.getLabelType() != null) {
            entity.setLabelType(request.getLabelType());
        }
        if (request.getWidthMm() != null) {
            entity.setWidthMm(request.getWidthMm());
        }
        if (request.getHeightMm() != null) {
            entity.setHeightMm(request.getHeightMm());
        }
        if (request.getDpi() != null) {
            entity.setDpi(request.getDpi());
        }
        if (request.getVariables() != null) {
            entity.setVariables(serializeVariables(request.getVariables()));
        }
        if (request.getPreviewImageUrl() != null) {
            entity.setPreviewImageUrl(request.getPreviewImageUrl());
        }
        if (request.getIsDefault() != null) {
            entity.setIsDefault(request.getIsDefault());
        }
    }

    public List<LabelTemplateResponse> toResponseList(List<LabelTemplate> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<LabelTemplateListResponse> toListResponseList(List<LabelTemplate> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }

    private List<String> parseVariables(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse variables JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String serializeVariables(List<String> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize variables: {}", e.getMessage());
            return null;
        }
    }
}
