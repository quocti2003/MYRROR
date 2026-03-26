package com.mirror.product.service;

import com.mirror.product.dto.workflow.*;
import com.mirror.product.entity.WorkflowStage;
import com.mirror.product.entity.WorkflowTemplate;
import com.mirror.product.enums.ProductionOrderStageStatus;
import com.mirror.product.enums.WorkflowTemplateStatus;
import com.mirror.product.mapper.WorkflowTemplateMapper;
import com.mirror.product.repository.ProductionPlanRepository;
import com.mirror.product.repository.WorkflowStageRepository;
import com.mirror.product.repository.WorkflowTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing Workflow Templates and Stages
 *
 * Stage Dependency Logic:
 * - First stage (stageOrder = 1) starts as READY
 * - Subsequent stages start as BLOCKED
 * - A stage becomes READY only when the previous stage completes
 * - Cannot start a stage unless its status is READY
 * - Completing a stage automatically sets the next stage to READY
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowTemplateService {

    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowStageRepository stageRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final WorkflowTemplateMapper mapper;

    // ==================== Template CRUD Operations ====================

    /**
     * Find template by ID with all stages loaded
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowTemplate> findByIdWithStages(String id) {
        return templateRepository.findActiveByIdWithStages(id);
    }

    /**
     * Find template by ID
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowTemplate> findById(String id) {
        return templateRepository.findActiveById(id);
    }

    /**
     * Find template by name
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowTemplate> findByName(String name) {
        return templateRepository.findActiveByName(name);
    }

    /**
     * Search templates with filters
     */
    @Transactional(readOnly = true)
    public Page<WorkflowTemplate> search(WorkflowTemplateSearchCriteria criteria, Pageable pageable) {
        if (criteria == null) {
            return templateRepository.findAllActiveWithFilters(null, null, pageable);
        }

        // If text search is provided, use the search method
        if (criteria.getSearch() != null && !criteria.getSearch().isEmpty()) {
            return templateRepository.searchActive(criteria.getSearch(), pageable);
        }

        // Otherwise, use filters
        return templateRepository.findAllActiveWithFilters(
                criteria.getCategory(),
                criteria.getStatus(),
                pageable
        );
    }

    /**
     * Find templates by category
     */
    @Transactional(readOnly = true)
    public List<WorkflowTemplate> findByCategory(String category) {
        return templateRepository.findActiveByCategory(category);
    }

    /**
     * Find the default template for a category
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowTemplate> findDefaultByCategory(String category) {
        return templateRepository.findDefaultByCategory(category);
    }

    /**
     * Find templates by status
     */
    @Transactional(readOnly = true)
    public List<WorkflowTemplate> findByStatus(WorkflowTemplateStatus status) {
        return templateRepository.findActiveByStatus(status);
    }

    /**
     * Get distinct categories
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctCategories() {
        return templateRepository.findDistinctCategories();
    }

    /**
     * Get all active templates (status = ACTIVE)
     */
    @Transactional(readOnly = true)
    public List<WorkflowTemplate> findAllActiveTemplates() {
        return templateRepository.findAllActiveTemplates();
    }

    /**
     * Create a new workflow template with stages
     */
    @Transactional
    public WorkflowTemplate create(WorkflowTemplateCreateRequest request, String userId) {
        log.info("Creating new workflow template: {}", request.getName());

        // Check for duplicate name
        if (templateRepository.existsActiveByName(request.getName())) {
            throw new IllegalArgumentException("Template with name '" + request.getName() + "' already exists");
        }

        // Validate stages
        validateStages(request.getStages());

        // Create template entity
        WorkflowTemplate template = mapper.toEntity(request);
        template.setCreatedBy(userId);
        template.setUpdatedBy(userId);

        // Handle default flag
        if (Boolean.TRUE.equals(template.getIsDefault()) && template.getCategory() != null) {
            clearOtherDefaults(template.getCategory(), null);
        }

        // Save
        template = templateRepository.save(template);

        log.info("Created workflow template with ID: {} and {} stages",
                template.getId(), template.getStageCount());
        return template;
    }

    /**
     * Update an existing workflow template
     */
    @Transactional
    public WorkflowTemplate update(String id, WorkflowTemplateUpdateRequest request, String userId) {
        log.info("Updating workflow template: {}", id);

        WorkflowTemplate template = templateRepository.findActiveByIdWithStages(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

        // Check if template can be edited
        if (template.getStatus() == WorkflowTemplateStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot update archived template");
        }

        // Check for name conflict
        if (request.getName() != null && !request.getName().equals(template.getName())) {
            if (templateRepository.existsActiveByNameAndIdNot(request.getName(), id)) {
                throw new IllegalArgumentException("Template with name '" + request.getName() + "' already exists");
            }
        }

        // Update basic fields
        mapper.updateEntity(template, request);
        template.setUpdatedBy(userId);

        // Handle default flag change
        if (Boolean.TRUE.equals(request.getIsDefault()) && template.getCategory() != null) {
            clearOtherDefaults(template.getCategory(), id);
        }

        // Update stages if provided
        if (request.getStages() != null) {
            validateStages(request.getStages());
            updateTemplateStages(template, request.getStages());
        }

        template = templateRepository.save(template);
        log.info("Updated workflow template: {}", id);

        return template;
    }

    /**
     * Update template status
     */
    @Transactional
    public WorkflowTemplate updateStatus(String id, WorkflowTemplateStatus newStatus, String userId, String reason) {
        log.info("User {} updating template status: id={}, newStatus={}, reason={}", userId, id, newStatus, reason);

        WorkflowTemplate template = templateRepository.findActiveByIdWithStages(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

        WorkflowTemplateStatus currentStatus = template.getStatus();

        // Validate status transition
        validateStatusTransition(template, currentStatus, newStatus);

        // Additional validation: Check if archiving a template that is in use by active production plans
        if (newStatus == WorkflowTemplateStatus.ARCHIVED && currentStatus == WorkflowTemplateStatus.ACTIVE) {
            long activePlanCount = productionPlanRepository.countByWorkflowTemplateId(id);
            if (activePlanCount > 0) {
                log.warn("User {} attempted to archive template {} that is used by {} active production plan(s)",
                    userId, id, activePlanCount);
                throw new IllegalStateException(
                    "Cannot archive workflow template: it is currently used by " + activePlanCount + " active production plan(s). " +
                    "Please complete or reassign these plans before archiving the template.");
            }
        }

        template.setStatus(newStatus);
        template.setUpdatedBy(userId);

        template = templateRepository.save(template);
        log.info("User {} updated template {} status from {} to {}", userId, id, currentStatus, newStatus);

        return template;
    }

    /**
     * Soft delete a template
     */
    @Transactional
    public void softDelete(String id, String userId) {
        log.info("User {} attempting to soft delete template: {}", userId, id);

        WorkflowTemplate template = templateRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

        // Validate: Cannot delete ACTIVE templates without archiving first
        if (template.getStatus() == WorkflowTemplateStatus.ACTIVE) {
            log.warn("User {} attempted to delete ACTIVE template {} without archiving first", userId, id);
            throw new IllegalStateException(
                "Cannot delete ACTIVE template. Please archive the template first before deletion.");
        }

        // Check if template is used by any non-deleted production plans
        long planCount = productionPlanRepository.countByWorkflowTemplateIdAndIsDeletedFalse(id);
        if (planCount > 0) {
            log.warn("User {} attempted to delete template {} that is used by {} production plan(s)",
                userId, id, planCount);
            throw new IllegalStateException(
                "Cannot delete workflow template: it is used by " + planCount + " production plan(s)");
        }

        template.setIsDeleted(true);
        template.setUpdatedBy(userId);
        templateRepository.save(template);

        log.info("User {} successfully soft deleted template: {}", userId, id);
    }

    /**
     * Duplicate a template
     */
    @Transactional
    public WorkflowTemplate duplicate(String id, String newName, String userId) {
        log.info("Duplicating template {} with new name: {}", id, newName);

        WorkflowTemplate source = templateRepository.findActiveByIdWithStages(id)
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
        WorkflowTemplate duplicate = mapper.createDuplicate(source, newName);
        duplicate.setCreatedBy(userId);
        duplicate.setUpdatedBy(userId);

        duplicate = templateRepository.save(duplicate);

        log.info("Created duplicate template with ID: {}", duplicate.getId());
        return duplicate;
    }

    // ==================== Stage Dependency Logic ====================

    /**
     * Determine the initial status for a stage based on its position in the workflow
     * First stage starts as READY, others start as BLOCKED
     */
    public ProductionOrderStageStatus getInitialStageStatus(int stageOrder) {
        return stageOrder == 1 ? ProductionOrderStageStatus.READY : ProductionOrderStageStatus.BLOCKED;
    }

    /**
     * Validate if a stage can transition to a new status based on dependency rules
     *
     * @param currentStatus Current status of the stage
     * @param newStatus     Desired new status
     * @return true if transition is valid
     */
    public boolean isValidStageStatusTransition(ProductionOrderStageStatus currentStatus, ProductionOrderStageStatus newStatus) {
        if (currentStatus == newStatus) {
            return true; // No change
        }

        return switch (currentStatus) {
            case BLOCKED ->
                // BLOCKED can only transition to READY (via previous stage completion) or SKIPPED
                    newStatus == ProductionOrderStageStatus.READY || newStatus == ProductionOrderStageStatus.SKIPPED;
            case READY ->
                // READY can transition to IN_PROGRESS (start work) or SKIPPED
                    newStatus == ProductionOrderStageStatus.IN_PROGRESS || newStatus == ProductionOrderStageStatus.SKIPPED;
            case IN_PROGRESS ->
                // IN_PROGRESS can transition to COMPLETED (finish work) or back to READY (pause)
                    newStatus == ProductionOrderStageStatus.COMPLETED || newStatus == ProductionOrderStageStatus.READY;
            case COMPLETED, SKIPPED ->
                // Terminal states - no transitions allowed
                    false;
        };
    }

    /**
     * Validate that a stage can be started (must be in READY status)
     */
    public void validateCanStartStage(ProductionOrderStageStatus currentStatus) {
        if (currentStatus != ProductionOrderStageStatus.READY) {
            throw new IllegalStateException(
                    "Stage can only be started when in READY status. Current status: " + currentStatus);
        }
    }

    /**
     * Validate that a stage can be completed (must be in IN_PROGRESS status)
     */
    public void validateCanCompleteStage(ProductionOrderStageStatus currentStatus) {
        if (currentStatus != ProductionOrderStageStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Stage can only be completed when in IN_PROGRESS status. Current status: " + currentStatus);
        }
    }

    /**
     * Get the next stage status after completing a stage
     * Returns READY for the next stage
     */
    public ProductionOrderStageStatus getNextStageStatusAfterCompletion() {
        return ProductionOrderStageStatus.READY;
    }

    /**
     * Validate stages for proper ordering and configuration
     */
    public void validateStages(List<WorkflowStageDTO> stages) {
        if (stages == null || stages.isEmpty()) {
            throw new IllegalArgumentException("At least one stage is required");
        }

        // Check for unique stage orders and names
        Set<Integer> orders = new HashSet<>();
        Set<String> names = new HashSet<>();
        boolean hasFinalStage = false;

        for (WorkflowStageDTO stage : stages) {
            if (stage.getStageOrder() == null || stage.getStageOrder() < 1) {
                throw new IllegalArgumentException("Stage order must be a positive integer");
            }
            if (!orders.add(stage.getStageOrder())) {
                throw new IllegalArgumentException("Duplicate stage order: " + stage.getStageOrder());
            }
            if (stage.getName() == null || stage.getName().isBlank()) {
                throw new IllegalArgumentException("Stage name is required");
            }
            if (!names.add(stage.getName().toLowerCase())) {
                throw new IllegalArgumentException("Duplicate stage name: " + stage.getName());
            }
            if (Boolean.TRUE.equals(stage.getIsFinalStage())) {
                if (hasFinalStage) {
                    throw new IllegalArgumentException("Only one stage can be marked as final");
                }
                hasFinalStage = true;
            }
        }

        // Validate sequential ordering (1, 2, 3, ...)
        List<Integer> sortedOrders = new ArrayList<>(orders);
        Collections.sort(sortedOrders);
        for (int i = 0; i < sortedOrders.size(); i++) {
            if (sortedOrders.get(i) != i + 1) {
                throw new IllegalArgumentException(
                        "Stage orders must be sequential starting from 1. Expected " + (i + 1) + ", found " + sortedOrders.get(i));
            }
        }
    }

    // ==================== Private Helper Methods ====================

    private void validateStatusTransition(WorkflowTemplate template, WorkflowTemplateStatus currentStatus, WorkflowTemplateStatus newStatus) {
        if (currentStatus == newStatus) {
            return; // No change
        }

        switch (currentStatus) {
            case DRAFT:
                if (newStatus != WorkflowTemplateStatus.ACTIVE && newStatus != WorkflowTemplateStatus.ARCHIVED) {
                    throw new IllegalStateException("DRAFT can only transition to ACTIVE or ARCHIVED");
                }
                // Validate template has stages before activating
                if (newStatus == WorkflowTemplateStatus.ACTIVE && (template.getStages() == null || template.getStages().isEmpty())) {
                    throw new IllegalStateException("Cannot activate template without stages");
                }
                break;
            case ACTIVE:
                if (newStatus != WorkflowTemplateStatus.ARCHIVED && newStatus != WorkflowTemplateStatus.DRAFT) {
                    throw new IllegalStateException("ACTIVE can only transition to ARCHIVED or back to DRAFT");
                }
                break;
            case ARCHIVED:
                throw new IllegalStateException("ARCHIVED templates cannot change status");
        }
    }

    private void updateTemplateStages(WorkflowTemplate template, List<WorkflowStageDTO> stageDTOs) {
        // Clear existing stages
        template.clearStages();

        // Add new stages
        for (WorkflowStageDTO stageDTO : stageDTOs) {
            WorkflowStage stage = mapper.toStageEntity(stageDTO);
            template.addStage(stage);
        }
    }

    private void clearOtherDefaults(String category, String excludeId) {
        List<WorkflowTemplate> otherDefaults = excludeId != null
                ? templateRepository.findOtherDefaultsInCategory(category, excludeId)
                : templateRepository.findActiveByCategory(category).stream()
                        .filter(t -> Boolean.TRUE.equals(t.getIsDefault()))
                        .toList();

        for (WorkflowTemplate other : otherDefaults) {
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
}
