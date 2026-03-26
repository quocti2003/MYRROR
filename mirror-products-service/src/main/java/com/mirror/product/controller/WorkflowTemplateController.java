package com.mirror.product.controller;

import com.mirror.product.dto.workflow.*;
import com.mirror.product.entity.WorkflowTemplate;
import com.mirror.product.enums.WorkflowTemplateStatus;
import com.mirror.product.mapper.WorkflowTemplateMapper;
import com.mirror.product.service.WorkflowTemplateService;
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
 * REST Controller for Workflow Template operations
 */
@RestController
@RequestMapping("/api/v1/workflow-templates")
@RequiredArgsConstructor
@Slf4j
public class WorkflowTemplateController {

    private final WorkflowTemplateService templateService;
    private final WorkflowTemplateMapper mapper;

    /**
     * Get all workflow templates with pagination and filters
     * GET /api/v1/workflow-templates
     */
    @GetMapping
    public ResponseEntity<?> getAllTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) WorkflowTemplateStatus status) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            WorkflowTemplateSearchCriteria criteria = WorkflowTemplateSearchCriteria.builder()
                    .search(search)
                    .category(category)
                    .status(status)
                    .build();

            Page<WorkflowTemplate> templatePage = templateService.search(criteria, pageable);
            List<WorkflowTemplateListResponse> responses = mapper.toListResponseList(templatePage.getContent());

            Map<String, Object> response = new HashMap<>();
            response.put("content", responses);
            response.put("currentPage", templatePage.getNumber());
            response.put("totalItems", templatePage.getTotalElements());
            response.put("totalPages", templatePage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting workflow templates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching workflow templates");
        }
    }

    /**
     * Get workflow template by ID with all stages
     * GET /api/v1/workflow-templates/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplateById(@PathVariable String id) {
        try {
            return templateService.findByIdWithStages(id)
                    .map(mapper::toResponse)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting workflow template by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching workflow template");
        }
    }

    /**
     * Create a new workflow template with stages
     * POST /api/v1/workflow-templates
     */
    @PostMapping
    public ResponseEntity<?> createTemplate(
            @Valid @RequestBody WorkflowTemplateCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            WorkflowTemplate template = templateService.create(request, userId);
            WorkflowTemplateResponse response = mapper.toResponse(template);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for creating workflow template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating workflow template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating workflow template");
        }
    }

    /**
     * Update an existing workflow template
     * PUT /api/v1/workflow-templates/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTemplate(
            @PathVariable String id,
            @Valid @RequestBody WorkflowTemplateUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            WorkflowTemplate template = templateService.update(id, request, userId);
            WorkflowTemplateResponse response = mapper.toResponse(template);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error updating workflow template {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating workflow template: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating workflow template");
        }
    }

    /**
     * Soft delete a workflow template
     * DELETE /api/v1/workflow-templates/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            templateService.softDelete(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot delete workflow template {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting workflow template: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while deleting workflow template");
        }
    }

    /**
     * Update workflow template status (Activate/Archive)
     * PATCH /api/v1/workflow-templates/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateTemplateStatus(
            @PathVariable String id,
            @Valid @RequestBody WorkflowTemplateStatusUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            WorkflowTemplate template = templateService.updateStatus(id, request.getStatus(), userId, request.getReason());
            WorkflowTemplateResponse response = mapper.toResponse(template);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.warn("Cannot update workflow template status {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error updating workflow template status {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating workflow template status: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating workflow template status");
        }
    }

    /**
     * Duplicate (clone) a workflow template
     * POST /api/v1/workflow-templates/{id}/duplicate
     */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<?> duplicateTemplate(
            @PathVariable String id,
            @RequestParam(required = false) String newName,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            WorkflowTemplate duplicate = templateService.duplicate(id, newName, userId);
            WorkflowTemplateResponse response = mapper.toResponse(duplicate);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error duplicating workflow template {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error duplicating workflow template: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while duplicating workflow template");
        }
    }

    /**
     * Get workflow templates by category
     * GET /api/v1/workflow-templates/by-category/{category}
     */
    @GetMapping("/by-category/{category}")
    public ResponseEntity<?> getTemplatesByCategory(@PathVariable String category) {
        try {
            List<WorkflowTemplate> templates = templateService.findByCategory(category);
            List<WorkflowTemplateListResponse> responses = mapper.toListResponseList(templates);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting workflow templates by category: {}", category, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching workflow templates");
        }
    }

    /**
     * Get the default workflow template for a category
     * GET /api/v1/workflow-templates/default/{category}
     */
    @GetMapping("/default/{category}")
    public ResponseEntity<?> getDefaultTemplateByCategory(@PathVariable String category) {
        try {
            return templateService.findDefaultByCategory(category)
                    .map(mapper::toResponse)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting default workflow template for category: {}", category, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching default workflow template");
        }
    }

    /**
     * Get workflow templates by status
     * GET /api/v1/workflow-templates/by-status/{status}
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<?> getTemplatesByStatus(@PathVariable WorkflowTemplateStatus status) {
        try {
            List<WorkflowTemplate> templates = templateService.findByStatus(status);
            List<WorkflowTemplateListResponse> responses = mapper.toListResponseList(templates);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting workflow templates by status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching workflow templates");
        }
    }

    /**
     * Get all active templates (status = ACTIVE)
     * GET /api/v1/workflow-templates/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveTemplates() {
        try {
            List<WorkflowTemplate> templates = templateService.findAllActiveTemplates();
            List<WorkflowTemplateListResponse> responses = mapper.toListResponseList(templates);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting active workflow templates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching active workflow templates");
        }
    }

    /**
     * Get distinct categories for dropdown
     * GET /api/v1/workflow-templates/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getDistinctCategories() {
        try {
            List<String> categories = templateService.getDistinctCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error getting distinct categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching categories");
        }
    }

    /**
     * Get workflow template count
     * GET /api/v1/workflow-templates/count
     */
    @GetMapping("/count")
    public ResponseEntity<?> getTemplateCount() {
        try {
            long count = templateService.countActive();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error getting workflow template count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching workflow template count");
        }
    }
}
