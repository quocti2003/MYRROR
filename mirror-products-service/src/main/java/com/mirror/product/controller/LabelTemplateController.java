package com.mirror.product.controller;

import com.mirror.product.dto.label.*;
import com.mirror.product.entity.LabelTemplate;
import com.mirror.product.enums.LabelType;
import com.mirror.product.mapper.LabelTemplateMapper;
import com.mirror.product.service.LabelTemplateService;
import com.mirror.product.service.LabelaryPreviewService;
import com.mirror.product.service.ZplRenderingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Label Template operations
 */
@RestController
@RequestMapping("/api/v1/label-templates")
@RequiredArgsConstructor
@Slf4j
public class LabelTemplateController {

    private final LabelTemplateService templateService;
    private final LabelTemplateMapper mapper;
    private final ZplRenderingService zplRenderingService;
    private final LabelaryPreviewService labelaryPreviewService;

    /**
     * Get all label templates with pagination and filters
     * GET /api/v1/label-templates
     */
    @GetMapping
    public ResponseEntity<?> getAllTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LabelType labelType) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<LabelTemplate> templatePage = templateService.search(search, labelType, pageable);
            List<LabelTemplateListResponse> responses = mapper.toListResponseList(templatePage.getContent());

            Map<String, Object> response = new HashMap<>();
            response.put("content", responses);
            response.put("currentPage", templatePage.getNumber());
            response.put("totalItems", templatePage.getTotalElements());
            response.put("totalPages", templatePage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting label templates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching label templates");
        }
    }

    /**
     * Get label template by ID
     * GET /api/v1/label-templates/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplateById(@PathVariable String id) {
        try {
            return templateService.findById(id)
                    .map(mapper::toResponse)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting label template by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching label template");
        }
    }

    /**
     * Create a new label template
     * POST /api/v1/label-templates
     */
    @PostMapping
    public ResponseEntity<?> createTemplate(@Valid @RequestBody LabelTemplateRequest request) {
        try {
            LabelTemplate template = templateService.create(request);
            LabelTemplateResponse response = mapper.toResponse(template);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for creating label template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating label template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating label template");
        }
    }

    /**
     * Update an existing label template
     * PUT /api/v1/label-templates/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTemplate(
            @PathVariable String id,
            @Valid @RequestBody LabelTemplateRequest request) {
        try {
            LabelTemplate template = templateService.update(id, request);
            LabelTemplateResponse response = mapper.toResponse(template);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error updating label template {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating label template: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating label template");
        }
    }

    /**
     * Soft delete a label template
     * DELETE /api/v1/label-templates/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable String id) {
        try {
            templateService.softDelete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting label template: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while deleting label template");
        }
    }

    /**
     * Duplicate (clone) a label template
     * POST /api/v1/label-templates/{id}/duplicate
     */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<?> duplicateTemplate(
            @PathVariable String id,
            @RequestParam(required = false) String newName) {
        try {
            LabelTemplate duplicate = templateService.duplicate(id, newName);
            LabelTemplateResponse response = mapper.toResponse(duplicate);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error duplicating label template {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error duplicating label template: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while duplicating label template");
        }
    }

    /**
     * Get label templates by type
     * GET /api/v1/label-templates/by-type/{labelType}
     */
    @GetMapping("/by-type/{labelType}")
    public ResponseEntity<?> getTemplatesByType(@PathVariable LabelType labelType) {
        try {
            List<LabelTemplate> templates = templateService.findByLabelType(labelType);
            List<LabelTemplateListResponse> responses = mapper.toListResponseList(templates);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting label templates by type: {}", labelType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching label templates");
        }
    }

    /**
     * Get the default template for a label type
     * GET /api/v1/label-templates/default/{labelType}
     */
    @GetMapping("/default/{labelType}")
    public ResponseEntity<?> getDefaultTemplateByType(@PathVariable LabelType labelType) {
        try {
            return templateService.findDefaultByLabelType(labelType)
                    .map(mapper::toResponse)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting default label template for type: {}", labelType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching default label template");
        }
    }

    /**
     * Get distinct label types for dropdown
     * GET /api/v1/label-templates/types
     */
    @GetMapping("/types")
    public ResponseEntity<?> getDistinctLabelTypes() {
        try {
            List<LabelType> types = templateService.getDistinctLabelTypes();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("Error getting distinct label types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching label types");
        }
    }

    /**
     * Get all label types (enum values)
     * GET /api/v1/label-templates/all-types
     */
    @GetMapping("/all-types")
    public ResponseEntity<?> getAllLabelTypes() {
        return ResponseEntity.ok(LabelType.values());
    }

    /**
     * Get all default templates
     * GET /api/v1/label-templates/defaults
     */
    @GetMapping("/defaults")
    public ResponseEntity<?> getAllDefaults() {
        try {
            List<LabelTemplate> templates = templateService.findAllDefaults();
            List<LabelTemplateListResponse> responses = mapper.toListResponseList(templates);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting default label templates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching default label templates");
        }
    }

    /**
     * Get supported variables list
     * GET /api/v1/label-templates/variables
     */
    @GetMapping("/variables")
    public ResponseEntity<?> getSupportedVariables() {
        return ResponseEntity.ok(zplRenderingService.getSupportedVariables());
    }

    /**
     * Get label template count
     * GET /api/v1/label-templates/count
     */
    @GetMapping("/count")
    public ResponseEntity<?> getTemplateCount() {
        try {
            long count = templateService.countActive();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error getting label template count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching label template count");
        }
    }

    /**
     * Generate a preview for a template with sample data
     * POST /api/v1/label-templates/{id}/preview
     */
    @PostMapping("/{id}/preview")
    public ResponseEntity<?> generatePreview(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> variables) {
        try {
            LabelTemplate template = templateService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

            String renderedZpl = zplRenderingService.renderZpl(template, variables);
            LabelPreviewResponse preview = labelaryPreviewService.generatePreviewResponse(template, renderedZpl);

            return ResponseEntity.ok(preview);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error generating preview for template {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error generating preview for template: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while generating preview");
        }
    }
}
