package com.mirror.product.service;

import com.mirror.product.dto.workflow.*;
import com.mirror.product.entity.CollectionPlan;
import com.mirror.product.entity.ProductionOrder;
import com.mirror.product.entity.ProductionOrderStage;
import com.mirror.product.entity.ProductionPlan;
import com.mirror.product.entity.WorkflowTemplate;
import com.mirror.product.enums.ProductionPlanStatus;
import com.mirror.product.enums.WorkflowTemplateStatus;
import com.mirror.product.mapper.ProductionPlanMapper;
import com.mirror.product.repository.CollectionPlanRepository;
import com.mirror.product.repository.ProductionOrderRepository;
import com.mirror.product.repository.ProductionPlanRepository;
import com.mirror.product.repository.WorkflowTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Production Plans
 *
 * Production Plans link CollectionPlans to WorkflowTemplates and track
 * the overall production progress for a collection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionPlanService {

    private final ProductionPlanRepository planRepository;
    private final ProductionOrderRepository orderRepository;
    private final WorkflowTemplateRepository templateRepository;
    private final CollectionPlanRepository collectionPlanRepository;
    private final ProductionPlanMapper mapper;

    // ==================== Find Operations ====================

    /**
     * Find plan by ID with workflow template loaded
     */
    @Transactional(readOnly = true)
    public Optional<ProductionPlan> findByIdWithWorkflowTemplate(String id) {
        return planRepository.findActiveByIdWithWorkflowTemplate(id);
    }

    /**
     * Find plan by ID
     */
    @Transactional(readOnly = true)
    public Optional<ProductionPlan> findById(String id) {
        return planRepository.findActiveById(id);
    }

    /**
     * Find plan by name
     */
    @Transactional(readOnly = true)
    public Optional<ProductionPlan> findByName(String name) {
        return planRepository.findActiveByName(name);
    }

    /**
     * Search plans with filters
     */
    @Transactional(readOnly = true)
    public Page<ProductionPlan> search(ProductionPlanSearchCriteria criteria, Pageable pageable) {
        if (criteria == null) {
            return planRepository.findAllActiveWithFilters(null, null, pageable);
        }

        // If text search is provided, use the search method
        if (criteria.getSearch() != null && !criteria.getSearch().isEmpty()) {
            return planRepository.searchActive(criteria.getSearch(), pageable);
        }

        // Otherwise, use filters
        return planRepository.findAllActiveWithFilters(
                criteria.getStatus(),
                criteria.getCollectionPlanId(),
                pageable
        );
    }

    /**
     * Find plans by collection plan
     */
    @Transactional(readOnly = true)
    public List<ProductionPlan> findByCollectionPlanId(UUID collectionPlanId) {
        return planRepository.findActiveByCollectionPlanId(collectionPlanId);
    }

    /**
     * Find plans by workflow template
     */
    @Transactional(readOnly = true)
    public List<ProductionPlan> findByWorkflowTemplateId(String templateId) {
        return planRepository.findActiveByWorkflowTemplateId(templateId);
    }

    /**
     * Find plans by status
     */
    @Transactional(readOnly = true)
    public List<ProductionPlan> findByStatus(ProductionPlanStatus status) {
        return planRepository.findActiveByStatus(status);
    }

    /**
     * Find all active plans (APPROVED or IN_PRODUCTION)
     */
    @Transactional(readOnly = true)
    public List<ProductionPlan> findAllActivePlans() {
        return planRepository.findAllActivePlans();
    }

    // ==================== Create Operations ====================

    /**
     * Create a new production plan
     */
    @Transactional
    public ProductionPlan create(ProductionPlanCreateRequest request, String userId) {
        log.info("Creating new production plan: {}", request.getName());

        // Validate workflow template
        WorkflowTemplate template = templateRepository.findActiveById(request.getWorkflowTemplateId())
                .orElseThrow(() -> new RuntimeException("Workflow template not found: " + request.getWorkflowTemplateId()));

        if (template.getStatus() != WorkflowTemplateStatus.ACTIVE) {
            throw new IllegalStateException("Cannot use a non-active workflow template");
        }

        // Validate collection plan if provided
        if (request.getCollectionPlanId() != null) {
            CollectionPlan collectionPlan = collectionPlanRepository.findById(request.getCollectionPlanId())
                    .orElseThrow(() -> new RuntimeException("Collection plan not found: " + request.getCollectionPlanId()));

            validateCollectionPlanState(collectionPlan);
        }

        // Validate dates
        validateDates(request.getTargetStartDate(), request.getTargetEndDate());

        // Create plan
        ProductionPlan plan = mapper.toEntity(request, template);
        plan.setCreatedBy(userId);
        plan.setUpdatedBy(userId);

        plan = planRepository.save(plan);

        log.info("Created production plan with ID: {}", plan.getId());
        return plan;
    }

    /**
     * Create a production plan from a collection plan (P2-09)
     */
    @Transactional
    public ProductionPlan createFromCollectionPlan(CreateFromCollectionPlanRequest request, String userId) {
        log.info("Creating production plan from collection plan: {}", request.getCollectionPlanId());

        // Find collection plan
        CollectionPlan collectionPlan = collectionPlanRepository.findById(request.getCollectionPlanId())
                .orElseThrow(() -> new RuntimeException("Collection plan not found: " + request.getCollectionPlanId()));

        // Validate collection plan state
        validateCollectionPlanState(collectionPlan);

        // Find workflow template
        WorkflowTemplate template = templateRepository.findActiveById(request.getWorkflowTemplateId())
                .orElseThrow(() -> new RuntimeException("Workflow template not found: " + request.getWorkflowTemplateId()));

        if (template.getStatus() != WorkflowTemplateStatus.ACTIVE) {
            throw new IllegalStateException("Cannot use a non-active workflow template");
        }

        // Validate dates
        validateDates(request.getTargetStartDate(), request.getTargetEndDate());

        // Create plan from collection plan
        ProductionPlan plan = mapper.toEntityFromCollectionPlan(request, collectionPlan, template);
        plan.setCreatedBy(userId);
        plan.setUpdatedBy(userId);

        plan = planRepository.save(plan);

        log.info("Created production plan {} from collection plan {}",
                plan.getId(), collectionPlan.getId());
        return plan;
    }

    // ==================== Update Operations ====================

    /**
     * Update an existing production plan
     */
    @Transactional
    public ProductionPlan update(String id, ProductionPlanUpdateRequest request, String userId) {
        log.info("Updating production plan: {}", id);

        ProductionPlan plan = planRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + id));

        // Check if plan can be edited
        if (!plan.isEditable()) {
            throw new IllegalStateException("Cannot update plan in status: " + plan.getStatus());
        }

        // Update workflow template if requested
        if (request.getWorkflowTemplateId() != null
                && !request.getWorkflowTemplateId().equals(plan.getWorkflowTemplate().getId())) {
            WorkflowTemplate newTemplate = templateRepository.findActiveById(request.getWorkflowTemplateId())
                    .orElseThrow(() -> new RuntimeException("Workflow template not found: " + request.getWorkflowTemplateId()));

            if (newTemplate.getStatus() != WorkflowTemplateStatus.ACTIVE) {
                throw new IllegalStateException("Cannot use a non-active workflow template");
            }

            plan.setWorkflowTemplate(newTemplate);
        }

        // Validate dates
        LocalDate startDate = request.getTargetStartDate() != null
                ? request.getTargetStartDate() : plan.getTargetStartDate();
        LocalDate endDate = request.getTargetEndDate() != null
                ? request.getTargetEndDate() : plan.getTargetEndDate();
        validateDates(startDate, endDate);

        // Update fields
        mapper.updateEntity(plan, request);
        plan.setUpdatedBy(userId);

        plan = planRepository.save(plan);
        log.info("Updated production plan: {}", id);

        return plan;
    }

    /**
     * Update production plan status
     */
    @Transactional
    public ProductionPlan updateStatus(String id, ProductionPlanStatus newStatus, String userId, String reason) {
        log.info("Updating plan status: id={}, newStatus={}, reason={}", id, newStatus, reason);

        ProductionPlan plan = planRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + id));

        ProductionPlanStatus currentStatus = plan.getStatus();

        // Validate status transition
        validateStatusTransition(currentStatus, newStatus);

        plan.setStatus(newStatus);
        plan.setUpdatedBy(userId);

        // Set actual dates based on status
        if (newStatus == ProductionPlanStatus.IN_PRODUCTION && plan.getActualStartDate() == null) {
            plan.setActualStartDate(LocalDate.now());
        } else if (newStatus == ProductionPlanStatus.COMPLETED && plan.getActualEndDate() == null) {
            plan.setActualEndDate(LocalDate.now());
        }

        plan = planRepository.save(plan);
        log.info("Updated plan {} status from {} to {}", id, currentStatus, newStatus);

        return plan;
    }

    /**
     * Soft delete a production plan
     */
    @Transactional
    public void softDelete(String id, String userId) {
        log.info("Soft deleting production plan: {}", id);

        ProductionPlan plan = planRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + id));

        // Only DRAFT and PLANNING plans can be deleted
        if (plan.getStatus() != ProductionPlanStatus.DRAFT
                && plan.getStatus() != ProductionPlanStatus.PLANNING) {
            throw new IllegalStateException("Only DRAFT or PLANNING plans can be deleted");
        }

        plan.setIsDeleted(true);
        plan.setUpdatedBy(userId);
        planRepository.save(plan);

        log.info("Soft deleted production plan: {}", id);
    }

    // ==================== Approval Flow (P3-09) ====================

    /**
     * Submit production plan for approval
     * Validates that the plan is ready for approval (has orders, etc.)
     */
    @Transactional
    public ProductionPlan submitForApproval(String id, String userId) {
        log.info("Submitting plan for approval: id={}, by={}", id, userId);

        ProductionPlan plan = planRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + id));

        if (plan.getStatus() != ProductionPlanStatus.DRAFT) {
            throw new IllegalStateException("Can only submit DRAFT plans for approval. Current status: " + plan.getStatus());
        }

        // Validate plan has production orders
        List<ProductionOrder> orders = orderRepository.findActiveByProductionPlanId(id);
        if (orders.isEmpty()) {
            throw new IllegalStateException("Cannot submit plan for approval: No production orders exist. Generate orders first.");
        }

        plan.setStatus(ProductionPlanStatus.PLANNING);
        plan.setUpdatedBy(userId);
        // Audit trail: append submission record to notes
        String auditNote = "[SUBMITTED FOR APPROVAL " + java.time.LocalDateTime.now() + " by " + userId + "]: " +
                orders.size() + " orders pending partner review.";
        plan.setNotes((plan.getNotes() != null ? plan.getNotes() + "\n" : "") + auditNote);
        plan = planRepository.save(plan);

        log.info("AUDIT: Plan {} submitted for approval by {}. {} orders pending review.",
                id, userId, orders.size());
        return plan;
    }

    /**
     * Approve production plan for production
     * Validates that all orders have partner assignments before approval
     *
     * @return Map with approval result and any warnings
     */
    @Transactional
    public Map<String, Object> approvePlan(String id, String approverId, boolean forceApprove) {
        log.info("Approving plan: id={}, approver={}, force={}", id, approverId, forceApprove);

        ProductionPlan plan = planRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + id));

        if (plan.getStatus() != ProductionPlanStatus.PLANNING) {
            throw new IllegalStateException("Can only approve PLANNING plans. Current status: " + plan.getStatus());
        }

        // Validate all stages have partner assignments
        List<ProductionOrder> orders = orderRepository.findActiveByProductionPlanId(id);
        if (orders.isEmpty()) {
            throw new IllegalStateException("Cannot approve plan: No production orders exist.");
        }

        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> unassignedStages = new ArrayList<>();
        int totalOrders = orders.size();
        int ordersWithAllAssignments = 0;

        for (ProductionOrder order : orders) {
            if (order.getStages() == null || order.getStages().isEmpty()) {
                continue;
            }

            boolean allAssigned = true;
            for (ProductionOrderStage stage : order.getStages()) {
                if (stage.getAssignedVendor() == null) {
                    allAssigned = false;
                    Map<String, String> unassigned = new HashMap<>();
                    unassigned.put("orderId", order.getId());
                    unassigned.put("orderNumber", order.getOrderNumber());
                    unassigned.put("stageId", stage.getId());
                    unassigned.put("stageName", stage.getStageName());
                    unassigned.put("requiredCapability", stage.getRequiredCapability() != null
                            ? stage.getRequiredCapability().name() : "N/A");
                    unassignedStages.add(unassigned);
                }
            }
            if (allAssigned) {
                ordersWithAllAssignments++;
            }
        }

        result.put("totalOrders", totalOrders);
        result.put("ordersWithAllAssignments", ordersWithAllAssignments);
        result.put("unassignedStages", unassignedStages);
        result.put("unassignedCount", unassignedStages.size());

        // If there are unassigned stages and not forcing approval
        if (!unassignedStages.isEmpty() && !forceApprove) {
            result.put("approved", false);
            result.put("message", "Plan has " + unassignedStages.size() +
                    " stages without partner assignments. Use forceApprove=true to approve anyway.");
            log.warn("Plan {} approval blocked: {} unassigned stages", id, unassignedStages.size());
            return result;
        }

        // Proceed with approval
        plan.setStatus(ProductionPlanStatus.APPROVED);
        plan.setUpdatedBy(approverId);
        // Audit trail: append approval record to notes
        String approvalNote = forceApprove
                ? "[FORCE APPROVED " + java.time.LocalDateTime.now() + " by " + approverId + "]: " +
                  unassignedStages.size() + " unassigned stages overridden."
                : "[APPROVED " + java.time.LocalDateTime.now() + " by " + approverId + "]: " +
                  "All " + totalOrders + " orders with partner assignments.";
        plan.setNotes((plan.getNotes() != null ? plan.getNotes() + "\n" : "") + approvalNote);
        plan = planRepository.save(plan);

        result.put("approved", true);
        result.put("approvedBy", approverId);
        result.put("approvedAt", java.time.LocalDateTime.now());
        result.put("planId", plan.getId());
        result.put("planName", plan.getName());
        result.put("message", unassignedStages.isEmpty()
                ? "Plan approved successfully. All stages have partner assignments."
                : "Plan approved with " + unassignedStages.size() + " unassigned stages (force approved).");

        log.info("AUDIT: Plan {} approved by {}. forceApprove={}, totalOrders={}, unassignedStages={}",
                id, approverId, forceApprove, totalOrders, unassignedStages.size());
        return result;
    }

    /**
     * Reject production plan back to DRAFT status
     */
    @Transactional
    public ProductionPlan rejectPlan(String id, String userId, String reason) {
        log.info("Rejecting plan: id={}, by={}, reason={}", id, userId, reason);

        ProductionPlan plan = planRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + id));

        if (plan.getStatus() != ProductionPlanStatus.PLANNING && plan.getStatus() != ProductionPlanStatus.APPROVED) {
            throw new IllegalStateException("Can only reject PLANNING or APPROVED plans. Current status: " + plan.getStatus());
        }

        plan.setStatus(ProductionPlanStatus.DRAFT);
        plan.setNotes((plan.getNotes() != null ? plan.getNotes() + "\n" : "") +
                "[REJECTED " + java.time.LocalDateTime.now() + " by " + userId + "]: " + reason);
        plan.setUpdatedBy(userId);
        plan = planRepository.save(plan);

        log.info("AUDIT: Plan {} rejected by {}. Reason: {}", id, userId, reason);
        return plan;
    }

    /**
     * Start production for an approved plan
     */
    @Transactional
    public ProductionPlan startProduction(String id, String userId) {
        log.info("Starting production for plan: id={}, by={}", id, userId);

        ProductionPlan plan = planRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + id));

        if (plan.getStatus() != ProductionPlanStatus.APPROVED) {
            throw new IllegalStateException("Can only start production for APPROVED plans. Current status: " + plan.getStatus());
        }

        plan.setStatus(ProductionPlanStatus.IN_PRODUCTION);
        plan.setActualStartDate(LocalDate.now());
        plan.setUpdatedBy(userId);
        // Audit trail: append production start record to notes
        String startNote = "[PRODUCTION STARTED " + java.time.LocalDateTime.now() + " by " + userId + "]: " +
                "Actual start date: " + plan.getActualStartDate();
        plan.setNotes((plan.getNotes() != null ? plan.getNotes() + "\n" : "") + startNote);
        plan = planRepository.save(plan);

        log.info("AUDIT: Production started for plan {} by {}. Actual start date: {}",
                id, userId, plan.getActualStartDate());
        return plan;
    }

    /**
     * Complete a production plan
     */
    @Transactional
    public ProductionPlan completePlan(String id, String userId) {
        log.info("Completing plan: id={}, by={}", id, userId);

        ProductionPlan plan = planRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + id));

        if (plan.getStatus() != ProductionPlanStatus.IN_PRODUCTION) {
            throw new IllegalStateException("Can only complete IN_PRODUCTION plans. Current status: " + plan.getStatus());
        }

        plan.setStatus(ProductionPlanStatus.COMPLETED);
        plan.setActualEndDate(LocalDate.now());
        plan.setUpdatedBy(userId);
        // Audit trail: append completion record to notes
        String completeNote = "[COMPLETED " + java.time.LocalDateTime.now() + " by " + userId + "]: " +
                "Actual end date: " + plan.getActualEndDate();
        plan.setNotes((plan.getNotes() != null ? plan.getNotes() + "\n" : "") + completeNote);
        plan = planRepository.save(plan);

        log.info("AUDIT: Plan {} completed by {}. Actual end date: {}", id, userId, plan.getActualEndDate());
        return plan;
    }

    /**
     * Get plan approval readiness status
     * Returns information about what's needed before the plan can be approved
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getApprovalReadiness(String id) {
        ProductionPlan plan = planRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + id));

        List<ProductionOrder> orders = orderRepository.findActiveByProductionPlanId(id);

        Map<String, Object> readiness = new HashMap<>();
        readiness.put("planId", id);
        readiness.put("planName", plan.getName());
        readiness.put("currentStatus", plan.getStatus().name());
        readiness.put("canSubmitForApproval", plan.getStatus() == ProductionPlanStatus.DRAFT);
        readiness.put("canApprove", plan.getStatus() == ProductionPlanStatus.PLANNING);
        readiness.put("canStartProduction", plan.getStatus() == ProductionPlanStatus.APPROVED);

        // Check orders
        readiness.put("hasOrders", !orders.isEmpty());
        readiness.put("orderCount", orders.size());

        // Check assignments
        int unassignedCount = 0;
        int totalStages = 0;
        for (ProductionOrder order : orders) {
            if (order.getStages() != null) {
                for (ProductionOrderStage stage : order.getStages()) {
                    totalStages++;
                    if (stage.getAssignedVendor() == null) {
                        unassignedCount++;
                    }
                }
            }
        }
        readiness.put("totalStages", totalStages);
        readiness.put("unassignedStages", unassignedCount);
        readiness.put("allStagesAssigned", unassignedCount == 0);

        // Determine overall readiness
        boolean isReady = !orders.isEmpty() && unassignedCount == 0;
        readiness.put("isReadyForApproval", isReady);

        List<String> blockers = new ArrayList<>();
        if (orders.isEmpty()) {
            blockers.add("No production orders generated");
        }
        if (unassignedCount > 0) {
            blockers.add(unassignedCount + " stages without partner assignments");
        }
        readiness.put("blockers", blockers);

        return readiness;
    }

    // ==================== Validation Methods ====================

    private void validateCollectionPlanState(CollectionPlan collectionPlan) {
        // Check if collection plan is in a valid state for production planning
        String status = collectionPlan.getStatus();
        if (status == null) {
            return; // Allow if no status is set
        }

        // Collection plans should typically be approved before production planning
        // But we allow some flexibility here
        if ("CANCELLED".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Cannot create production plan from a cancelled collection plan");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    private void validateStatusTransition(ProductionPlanStatus currentStatus, ProductionPlanStatus newStatus) {
        if (currentStatus == newStatus) {
            return; // No change
        }

        boolean isValid = switch (currentStatus) {
            case DRAFT -> newStatus == ProductionPlanStatus.PLANNING
                    || newStatus == ProductionPlanStatus.CANCELLED;
            case PLANNING -> newStatus == ProductionPlanStatus.APPROVED
                    || newStatus == ProductionPlanStatus.DRAFT
                    || newStatus == ProductionPlanStatus.CANCELLED;
            case APPROVED -> newStatus == ProductionPlanStatus.IN_PRODUCTION
                    || newStatus == ProductionPlanStatus.PLANNING
                    || newStatus == ProductionPlanStatus.CANCELLED;
            case IN_PRODUCTION -> newStatus == ProductionPlanStatus.COMPLETED
                    || newStatus == ProductionPlanStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false; // Terminal states
        };

        if (!isValid) {
            throw new IllegalStateException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Count active plans
     */
    @Transactional(readOnly = true)
    public long countActive() {
        return planRepository.countActive();
    }

    /**
     * Check if plan exists by ID
     */
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return planRepository.existsActiveById(id);
    }

    /**
     * Check if a collection plan has any production plans
     */
    @Transactional(readOnly = true)
    public boolean hasProductionPlans(UUID collectionPlanId) {
        return planRepository.existsByCollectionPlanId(collectionPlanId);
    }

    /**
     * Get collection plan for a production plan
     */
    @Transactional(readOnly = true)
    public Optional<CollectionPlan> getCollectionPlan(String planId) {
        return planRepository.findActiveById(planId)
                .map(ProductionPlan::getCollectionPlanId)
                .flatMap(collectionPlanRepository::findById);
    }
}
