package com.mirror.product.service;

import com.mirror.product.dto.label.LabelTemplateRequest;
import com.mirror.product.entity.LabelTemplate;
import com.mirror.product.enums.LabelType;
import com.mirror.product.mapper.LabelTemplateMapper;
import com.mirror.product.repository.LabelTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing Label Templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabelTemplateService {

    private final LabelTemplateRepository templateRepository;
    private final LabelTemplateMapper mapper;

    /**
     * Find template by ID
     */
    @Transactional(readOnly = true)
    public Optional<LabelTemplate> findById(String id) {
        return templateRepository.findActiveById(id);
    }

    /**
     * Find template by name
     */
    @Transactional(readOnly = true)
    public Optional<LabelTemplate> findByName(String name) {
        return templateRepository.findActiveByName(name);
    }

    /**
     * Find all active templates
     */
    @Transactional(readOnly = true)
    public List<LabelTemplate> findAllActive() {
        return templateRepository.findAllActive();
    }

    /**
     * Search templates with filters
     */
    @Transactional(readOnly = true)
    public Page<LabelTemplate> search(String searchTerm, LabelType labelType, Pageable pageable) {
        if (searchTerm != null && !searchTerm.isBlank()) {
            return templateRepository.searchActive(searchTerm, pageable);
        }
        return templateRepository.findAllActiveWithFilters(labelType, pageable);
    }

    /**
     * Find templates by label type
     */
    @Transactional(readOnly = true)
    public List<LabelTemplate> findByLabelType(LabelType labelType) {
        return templateRepository.findActiveByLabelType(labelType);
    }

    /**
     * Find the default template for a label type
     */
    @Transactional(readOnly = true)
    public Optional<LabelTemplate> findDefaultByLabelType(LabelType labelType) {
        return templateRepository.findDefaultByLabelType(labelType);
    }

    /**
     * Get distinct label types that have templates
     */
    @Transactional(readOnly = true)
    public List<LabelType> getDistinctLabelTypes() {
        return templateRepository.findDistinctLabelTypes();
    }

    /**
     * Get all default templates
     */
    @Transactional(readOnly = true)
    public List<LabelTemplate> findAllDefaults() {
        return templateRepository.findAllDefaults();
    }

    /**
     * Create a new label template
     */
    @Transactional
    public LabelTemplate create(LabelTemplateRequest request) {
        log.info("Creating new label template: {}", request.getName());

        // Check for duplicate name
        if (templateRepository.existsActiveByName(request.getName())) {
            throw new IllegalArgumentException("Template with name '" + request.getName() + "' already exists");
        }

        // Create entity
        LabelTemplate template = mapper.toEntity(request);

        // Handle default flag
        if (Boolean.TRUE.equals(template.getIsDefault()) && template.getLabelType() != null) {
            clearOtherDefaults(template.getLabelType(), null);
        }

        // Save
        template = templateRepository.save(template);

        log.info("Created label template with ID: {}", template.getId());
        return template;
    }

    /**
     * Update an existing label template
     */
    @Transactional
    public LabelTemplate update(String id, LabelTemplateRequest request) {
        log.info("Updating label template: {}", id);

        LabelTemplate template = templateRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

        // Check for name conflict
        if (request.getName() != null && !request.getName().equals(template.getName())) {
            if (templateRepository.existsActiveByNameAndIdNot(request.getName(), id)) {
                throw new IllegalArgumentException("Template with name '" + request.getName() + "' already exists");
            }
        }

        // Update fields
        mapper.updateEntity(template, request);

        // Handle default flag change
        if (Boolean.TRUE.equals(request.getIsDefault()) && template.getLabelType() != null) {
            clearOtherDefaults(template.getLabelType(), id);
        }

        template = templateRepository.save(template);
        log.info("Updated label template: {}", id);

        return template;
    }

    /**
     * Soft delete a template
     */
    @Transactional
    public void softDelete(String id) {
        log.info("Soft deleting label template: {}", id);

        LabelTemplate template = templateRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

        template.setIsDeleted(true);
        templateRepository.save(template);

        log.info("Soft deleted label template: {}", id);
    }

    /**
     * Duplicate a template
     */
    @Transactional
    public LabelTemplate duplicate(String id, String newName) {
        log.info("Duplicating template {} with new name: {}", id, newName);

        LabelTemplate source = templateRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

        // Generate unique name if not provided
        if (newName == null || newName.isBlank()) {
            newName = generateDuplicateName(source.getName());
        }

        // Check for duplicate name
        if (templateRepository.existsActiveByName(newName)) {
            throw new IllegalArgumentException("Template with name '" + newName + "' already exists");
        }

        // Create duplicate
        LabelTemplate duplicate = LabelTemplate.builder()
                .name(newName)
                .description(source.getDescription())
                .zplContent(source.getZplContent())
                .labelType(source.getLabelType())
                .widthMm(source.getWidthMm())
                .heightMm(source.getHeightMm())
                .dpi(source.getDpi())
                .variables(source.getVariables())
                .previewImageUrl(source.getPreviewImageUrl())
                .isDefault(false) // Duplicates are never default
                .build();

        duplicate = templateRepository.save(duplicate);

        log.info("Created duplicate template with ID: {}", duplicate.getId());
        return duplicate;
    }

    /**
     * Count active templates
     */
    @Transactional(readOnly = true)
    public long countActive() {
        return templateRepository.countActive();
    }

    /**
     * Check if template exists by ID
     */
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return templateRepository.existsActiveById(id);
    }

    // ==================== Private Helper Methods ====================

    private void clearOtherDefaults(LabelType labelType, String excludeId) {
        List<LabelTemplate> otherDefaults = excludeId != null
                ? templateRepository.findOtherDefaultsForLabelType(labelType, excludeId)
                : templateRepository.findActiveByLabelType(labelType).stream()
                        .filter(t -> Boolean.TRUE.equals(t.getIsDefault()))
                        .toList();

        for (LabelTemplate other : otherDefaults) {
            other.setIsDefault(false);
            templateRepository.save(other);
        }
    }

    private String generateDuplicateName(String originalName) {
        String baseName = originalName + " (Copy)";
        int counter = 1;
        String newName = baseName;

        while (templateRepository.existsActiveByName(newName)) {
            counter++;
            newName = originalName + " (Copy " + counter + ")";
        }

        return newName;
    }
}
