package com.mirror.product.service.label;

import com.mirror.product.dto.label.LabelTemplateCreateRequest;
import com.mirror.product.dto.label.LabelTemplateResponse;
import com.mirror.product.dto.label.LabelTemplateUpdateRequest;
import com.mirror.product.entity.label.LabelTemplate;
import com.mirror.product.enums.LabelTemplateStatus;
import com.mirror.product.repository.label.RFIDLabelTemplateRepository;
import com.mirror.product.service.R2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RFIDLabelTemplateService {

    private final RFIDLabelTemplateRepository templateRepository;
    private final R2Service r2Service;

    /**
     * Create a new label template
     */
    @Transactional
    public LabelTemplateResponse create(LabelTemplateCreateRequest request, String userId) {
        // Check if name already exists
        if (templateRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new IllegalStateException("Template with name '" + request.getName() + "' already exists");
        }

        LabelTemplate template = LabelTemplate.builder()
            .name(request.getName())
            .description(request.getDescription())
            .labelWidth(request.getLabelWidth())
            .labelHeight(request.getLabelHeight())
            .dpi(request.getDpi() != null ? request.getDpi() : 300)
            .canvasJson(request.getCanvasJson())
            .isDefault(request.getIsDefault() != null && request.getIsDefault())
            .status(LabelTemplateStatus.ACTIVE)
            .createdBy(userId)
            .updatedBy(userId)
            .build();

        // Handle default flag
        if (Boolean.TRUE.equals(template.getIsDefault())) {
            clearDefaultTemplate();
        }

        template = templateRepository.save(template);

        // Upload preview image if provided
        if (request.getPreviewImage() != null && !request.getPreviewImage().isEmpty()) {
            String previewUrl = uploadPreviewImage(template.getId(), request.getPreviewImage());
            template.setPreviewUrl(previewUrl);
            template = templateRepository.save(template);
        }

        log.info("Created label template: {} by user {}", template.getId(), userId);
        return LabelTemplateResponse.fromEntity(template);
    }

    /**
     * Update an existing template
     */
    @Transactional
    public LabelTemplateResponse update(String id, LabelTemplateUpdateRequest request, String userId) {
        LabelTemplate template = templateRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        if (request.getName() != null) {
            template.setName(request.getName());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getLabelWidth() != null) {
            template.setLabelWidth(request.getLabelWidth());
        }
        if (request.getLabelHeight() != null) {
            template.setLabelHeight(request.getLabelHeight());
        }
        if (request.getDpi() != null) {
            template.setDpi(request.getDpi());
        }
        if (request.getCanvasJson() != null) {
            template.setCanvasJson(request.getCanvasJson());
        }
        if (request.getIsDefault() != null) {
            if (Boolean.TRUE.equals(request.getIsDefault())) {
                clearDefaultTemplate();
            }
            template.setIsDefault(request.getIsDefault());
        }
        if (request.getStatus() != null) {
            template.setStatus(LabelTemplateStatus.valueOf(request.getStatus()));
        }

        template.setUpdatedBy(userId);

        // Upload new preview image if provided
        if (request.getPreviewImage() != null && !request.getPreviewImage().isEmpty()) {
            String previewUrl = uploadPreviewImage(template.getId(), request.getPreviewImage());
            template.setPreviewUrl(previewUrl);
        }

        template = templateRepository.save(template);
        log.info("Updated label template: {} by user {}", template.getId(), userId);
        return LabelTemplateResponse.fromEntity(template);
    }

    /**
     * Get template by ID
     */
    public LabelTemplateResponse getById(String id) {
        LabelTemplate template = templateRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Template not found: " + id));
        return LabelTemplateResponse.fromEntity(template);
    }

    /**
     * Get all templates with pagination and filters
     */
    public Page<LabelTemplateResponse> getAll(LabelTemplateStatus status, String search, Pageable pageable) {
        Page<LabelTemplate> templates = templateRepository.findWithFilters(status, search, pageable);
        return templates.map(LabelTemplateResponse::fromEntity);
    }

    /**
     * Delete template (soft delete)
     */
    @Transactional
    public void delete(String id, String userId) {
        LabelTemplate template = templateRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        template.setIsDeleted(true);
        template.setStatus(LabelTemplateStatus.DELETED);
        template.setUpdatedBy(userId);
        templateRepository.save(template);

        log.info("Deleted label template: {} by user {}", id, userId);
    }

    /**
     * Duplicate template
     */
    @Transactional
    public LabelTemplateResponse duplicate(String id, String userId) {
        LabelTemplate original = templateRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        String newName = original.getName() + " (Copy)";
        int counter = 1;
        while (templateRepository.existsByNameAndIsDeletedFalse(newName)) {
            newName = original.getName() + " (Copy " + counter + ")";
            counter++;
        }

        LabelTemplate copy = LabelTemplate.builder()
            .name(newName)
            .description(original.getDescription())
            .labelWidth(original.getLabelWidth())
            .labelHeight(original.getLabelHeight())
            .dpi(original.getDpi())
            .canvasJson(original.getCanvasJson())
            .previewUrl(original.getPreviewUrl())
            .isDefault(false)
            .status(LabelTemplateStatus.ACTIVE)
            .createdBy(userId)
            .updatedBy(userId)
            .build();

        copy = templateRepository.save(copy);
        log.info("Duplicated label template {} to {} by user {}", id, copy.getId(), userId);
        return LabelTemplateResponse.fromEntity(copy);
    }

    /**
     * Get default template
     */
    public Optional<LabelTemplateResponse> getDefault() {
        return templateRepository.findByIsDefaultTrueAndIsDeletedFalse()
            .map(LabelTemplateResponse::fromEntity);
    }

    /**
     * Get template entity by ID
     */
    public LabelTemplate getEntityById(String id) {
        return templateRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Template not found: " + id));
    }

    /**
     * Clear default flag from all templates
     */
    private void clearDefaultTemplate() {
        templateRepository.findByIsDefaultTrueAndIsDeletedFalse()
            .ifPresent(t -> {
                t.setIsDefault(false);
                templateRepository.save(t);
            });
    }

    /**
     * Upload preview image to R2
     */
    private String uploadPreviewImage(String templateId, String base64Image) {
        try {
            // Remove data URL prefix if present
            String base64Data = base64Image;
            if (base64Image.contains(",")) {
                base64Data = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            String key = "label-templates/" + templateId + "/preview.png";

            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(imageBytes);
            var response = r2Service.uploadFile(key, inputStream, imageBytes.length, "image/png");
            return response.getPublicUrl();
        } catch (Exception e) {
            log.error("Error uploading preview image for template {}", templateId, e);
            return null;
        }
    }
}
