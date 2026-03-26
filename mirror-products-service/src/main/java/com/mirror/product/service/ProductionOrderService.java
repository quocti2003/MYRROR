package com.mirror.product.service;

import com.mirror.product.dto.productionorder.*;
import com.mirror.product.dto.productionorder.BulkAssignRequest.StageAssignment;
import com.mirror.product.entity.*;
import com.mirror.product.enums.HandoffStatus;
import com.mirror.product.enums.HandoffType;
import com.mirror.product.enums.ProductionOrderStageStatus;
import com.mirror.product.enums.ProductionOrderStatus;
import com.mirror.product.enums.ProductionPlanStatus;
import com.mirror.product.mapper.ProductionOrderMapper;
import com.mirror.product.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Production Orders and their stages
 *
 * Handles:
 * - Order CRUD operations
 * - Stage lifecycle management (start, complete, skip)
 * - Auto-generation of orders from production plans
 * - Order number generation (format: PO-YYYY-MM-####)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionOrderService {

    private final ProductionOrderRepository orderRepository;
    private final ProductionOrderStageRepository stageRepository;
    private final ProductionPlanRepository planRepository;
    private final WorkflowTemplateRepository templateRepository;
    private final VendorRepository vendorRepository;
    private final JewelryTechnicalReportRepository jtrcRepository;
    private final CollectionPlanItemRepository collectionPlanItemRepository;
    private final PartnerCapabilityRepository capabilityRepository;
    private final ComponentOwnershipLogRepository ownershipLogRepository;
    private final ProductionOrderMapper mapper;
    private final WorkflowTemplateService workflowTemplateService;
    private final JTRCProductLinkService jtrcProductLinkService;

    // ==================== Find Operations ====================

    @Transactional(readOnly = true)
    public Optional<ProductionOrder> findById(String id) {
        return orderRepository.findActiveById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ProductionOrder> findByIdWithStages(String id) {
        return orderRepository.findActiveByIdWithStages(id);
    }

    @Transactional(readOnly = true)
    public Optional<ProductionOrder> findByOrderNumber(String orderNumber) {
        return orderRepository.findActiveByOrderNumber(orderNumber);
    }

    @Transactional(readOnly = true)
    public List<ProductionOrder> findByProductionPlanId(String planId) {
        return orderRepository.findActiveByProductionPlanId(planId);
    }

    @Transactional(readOnly = true)
    public List<ProductionOrder> findByStatus(ProductionOrderStatus status) {
        return orderRepository.findActiveByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<ProductionOrder> findByCurrentHolderId(String vendorId) {
        return orderRepository.findActiveByCurrentHolderId(vendorId);
    }

    @Transactional(readOnly = true)
    public Page<ProductionOrder> search(ProductionOrderSearchCriteria criteria, Pageable pageable) {
        if (criteria == null) {
            return orderRepository.findAllActiveWithFilters(null, null, null, pageable);
        }

        if (criteria.getSearch() != null && !criteria.getSearch().isEmpty()) {
            return orderRepository.searchActive(criteria.getSearch(), pageable);
        }

        return orderRepository.findAllActiveWithFilters(
                criteria.getProductionPlanId(),
                criteria.getStatus(),
                criteria.getVendorId(),
                pageable
        );
    }

    // ==================== Create Operations ====================

    @Transactional
    public ProductionOrder create(ProductionOrderCreateRequest request, String userId) {
        log.info("Creating new production order for plan: {}", request.getProductionPlanId());

        // Validate production plan
        ProductionPlan plan = planRepository.findActiveByIdWithWorkflowTemplate(request.getProductionPlanId())
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + request.getProductionPlanId()));

        // Create order
        ProductionOrder order = mapper.toEntity(request, plan);
        order.setOrderNumber(generateOrderNumber());
        order.setCreatedBy(userId);
        order.setUpdatedBy(userId);

        // Set JTRC if provided
        if (request.getJtrcId() != null) {
            JewelryTechnicalReport jtrc = jtrcRepository.findActiveById(request.getJtrcId())
                    .orElseThrow(() -> new RuntimeException("JTRC not found: " + request.getJtrcId()));
            order.setJtrc(jtrc);
        }

        // Save order first to get ID
        order = orderRepository.save(order);

        // Create stages from workflow template
        createStagesFromTemplate(order, plan.getWorkflowTemplate());

        // Set current stage to first stage
        if (!order.getStages().isEmpty()) {
            order.setCurrentStageOrder(1);
        }

        order = orderRepository.save(order);

        log.info("Created production order {} with {} stages", order.getId(), order.getStages().size());
        return order;
    }

    // ==================== Update Operations ====================

    @Transactional
    public ProductionOrder update(String id, ProductionOrderUpdateRequest request, String userId) {
        log.info("Updating production order: {}", id);

        ProductionOrder order = orderRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Production order not found: " + id));

        if (!order.isEditable()) {
            throw new IllegalStateException("Cannot update order in status: " + order.getStatus());
        }

        // Update JTRC if provided
        if (request.getJtrcId() != null && !request.getJtrcId().equals(
                order.getJtrc() != null ? order.getJtrc().getId() : null)) {
            JewelryTechnicalReport jtrc = jtrcRepository.findActiveById(request.getJtrcId())
                    .orElseThrow(() -> new RuntimeException("JTRC not found: " + request.getJtrcId()));
            order.setJtrc(jtrc);
        }

        mapper.updateEntity(order, request);
        order.setUpdatedBy(userId);

        order = orderRepository.save(order);
        log.info("Updated production order: {}", id);

        return order;
    }

    @Transactional
    public ProductionOrder updateStatus(String id, ProductionOrderStatus newStatus, String userId, String reason) {
        log.info("Updating order status: id={}, newStatus={}", id, newStatus);

        ProductionOrder order = orderRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Production order not found: " + id));

        ProductionOrderStatus currentStatus = order.getStatus();
        validateOrderStatusTransition(currentStatus, newStatus);

        order.setStatus(newStatus);
        order.setUpdatedBy(userId);

        // Set completion date if completed
        if (newStatus == ProductionOrderStatus.COMPLETED && order.getActualCompletionDate() == null) {
            order.setActualCompletionDate(LocalDate.now());
        }

        order = orderRepository.save(order);
        log.info("Updated order {} status from {} to {}", id, currentStatus, newStatus);

        return order;
    }

    @Transactional
    public void softDelete(String id, String userId) {
        log.info("Soft deleting production order: {}", id);

        ProductionOrder order = orderRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Production order not found: " + id));

        if (!order.isEditable()) {
            throw new IllegalStateException("Cannot delete order in status: " + order.getStatus());
        }

        order.setIsDeleted(true);
        order.setUpdatedBy(userId);
        orderRepository.save(order);

        log.info("Soft deleted production order: {}", id);
    }

    // ==================== Stage Lifecycle Operations ====================

    @Transactional
    public ProductionOrderStage startStage(String orderId, String stageId, String userId) {
        log.info("Starting stage {} for order {}", stageId, orderId);

        ProductionOrderStage stage = stageRepository.findActiveByIdWithOrder(stageId)
                .orElseThrow(() -> new RuntimeException("Stage not found: " + stageId));

        // Validate order ownership
        if (!stage.getProductionOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("Stage does not belong to the specified order");
        }

        // Validate stage can be started
        workflowTemplateService.validateCanStartStage(stage.getStatus());

        // Update stage
        stage.setStatus(ProductionOrderStageStatus.IN_PROGRESS);
        stage.setActualStartDate(LocalDate.now());

        // Update order status if this is the first stage starting
        ProductionOrder order = stage.getProductionOrder();
        if (order.getStatus() == ProductionOrderStatus.READY) {
            order.setStatus(ProductionOrderStatus.IN_PROGRESS);
        }
        order.setCurrentStageOrder(stage.getStageOrder());

        // Update current holder if vendor is assigned
        if (stage.getAssignedVendor() != null) {
            order.setCurrentHolderId(stage.getAssignedVendor().getId());
        }
        order.setUpdatedBy(userId);

        stage = stageRepository.save(stage);
        orderRepository.save(order);

        // Create handoff log for stage start (component transfer to assigned vendor)
        if (stage.getAssignedVendor() != null) {
            createStageStartHandoff(order, stage, userId);
        }

        log.info("Started stage {} - now IN_PROGRESS", stageId);
        return stage;
    }

    @Transactional
    public ProductionOrderStage completeStage(String orderId, String stageId, StageCompleteRequest request, String userId) {
        log.info("Completing stage {} for order {}", stageId, orderId);

        ProductionOrderStage stage = stageRepository.findActiveByIdWithOrder(stageId)
                .orElseThrow(() -> new RuntimeException("Stage not found: " + stageId));

        // Validate order ownership
        if (!stage.getProductionOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("Stage does not belong to the specified order");
        }

        // Validate stage can be completed
        workflowTemplateService.validateCanCompleteStage(stage.getStatus());

        // Update stage
        stage.setStatus(ProductionOrderStageStatus.COMPLETED);
        stage.setActualEndDate(LocalDate.now());
        stage.setCompletedBy(userId);
        stage.setCompletedAt(LocalDateTime.now());

        if (request != null) {
            if (request.getActualCost() != null) {
                stage.setActualCost(request.getActualCost());
            }
            if (request.getNotes() != null) {
                stage.setNotes(request.getNotes());
            }
        }

        stage = stageRepository.save(stage);

        // Handle next stage and order completion
        ProductionOrder order = stage.getProductionOrder();
        handleStageCompletion(order, stage, userId);

        log.info("Completed stage {} - now COMPLETED", stageId);
        return stage;
    }

    @Transactional
    public ProductionOrderStage skipStage(String orderId, String stageId, String userId, String reason) {
        log.info("Skipping stage {} for order {} - reason: {}", stageId, orderId, reason);

        ProductionOrderStage stage = stageRepository.findActiveByIdWithOrder(stageId)
                .orElseThrow(() -> new RuntimeException("Stage not found: " + stageId));

        // Validate order ownership
        if (!stage.getProductionOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("Stage does not belong to the specified order");
        }

        // Validate stage can be skipped
        if (!stage.canSkip()) {
            throw new IllegalStateException("Stage cannot be skipped in status: " + stage.getStatus());
        }

        // Cannot skip the final stage - it must be completed
        if (Boolean.TRUE.equals(stage.getIsFinalStage())) {
            throw new IllegalStateException("Cannot skip the final stage - final delivery must be completed");
        }

        // Update stage
        stage.setStatus(ProductionOrderStageStatus.SKIPPED);
        stage.setNotes(reason);
        stage.setCompletedBy(userId);
        stage.setCompletedAt(LocalDateTime.now());

        stage = stageRepository.save(stage);

        // Handle next stage
        ProductionOrder order = stage.getProductionOrder();
        handleStageCompletion(order, stage, userId);

        log.info("Skipped stage {} - now SKIPPED", stageId);
        return stage;
    }

    @Transactional
    public ProductionOrderStage assignVendorToStage(String orderId, String stageId, StageAssignVendorRequest request, String userId) {
        log.info("Assigning vendor {} to stage {}", request.getVendorId(), stageId);

        ProductionOrderStage stage = stageRepository.findActiveByIdWithOrder(stageId)
                .orElseThrow(() -> new RuntimeException("Stage not found: " + stageId));

        // Validate order ownership
        if (!stage.getProductionOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("Stage does not belong to the specified order");
        }

        // Cannot assign vendor to completed/skipped stage
        if (stage.isTerminal()) {
            throw new IllegalStateException("Cannot assign vendor to a completed or skipped stage");
        }

        // Find vendor
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + request.getVendorId()));

        // Validate vendor has required capability for this stage
        if (stage.getRequiredCapability() != null) {
            boolean hasCapability = capabilityRepository.existsActiveByVendorIdAndCapabilityType(
                    vendor.getId(), stage.getRequiredCapability());
            if (!hasCapability) {
                throw new IllegalArgumentException(String.format(
                        "Vendor '%s' does not have the required capability '%s' for this stage",
                        vendor.getName(), stage.getRequiredCapability()));
            }
        }

        // Update stage
        stage.setAssignedVendor(vendor);
        if (request.getPlannedStartDate() != null) {
            stage.setPlannedStartDate(request.getPlannedStartDate());
        }
        if (request.getPlannedEndDate() != null) {
            stage.setPlannedEndDate(request.getPlannedEndDate());
        }
        if (request.getEstimatedCost() != null) {
            stage.setEstimatedCost(request.getEstimatedCost());
        }
        if (request.getNotes() != null) {
            stage.setNotes(request.getNotes());
        }

        stage = stageRepository.save(stage);

        log.info("Assigned vendor {} to stage {}", request.getVendorId(), stageId);
        return stage;
    }

    // ==================== Generate Orders from Plan ====================

    @Transactional
    public GenerateOrdersResponse generateOrdersFromPlan(String planId, GenerateOrdersRequest request, String userId) {
        log.info("Generating orders from production plan: {}", planId);

        // Find plan with workflow template
        ProductionPlan plan = planRepository.findActiveByIdWithWorkflowTemplate(planId)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + planId));

        // Validate plan can generate orders
        if (plan.getStatus() != ProductionPlanStatus.APPROVED && plan.getStatus() != ProductionPlanStatus.DRAFT
                && plan.getStatus() != ProductionPlanStatus.PLANNING) {
            throw new IllegalStateException("Cannot generate orders from plan in status: " + plan.getStatus());
        }

        WorkflowTemplate template = plan.getWorkflowTemplate();
        if (template == null || template.getStages() == null || template.getStages().isEmpty()) {
            throw new IllegalStateException("Production plan has no workflow template or template has no stages");
        }

        // Load template stages
        template = templateRepository.findActiveByIdWithStages(template.getId())
                .orElseThrow(() -> new RuntimeException("Workflow template not found"));

        List<ProductionOrder> generatedOrders = new ArrayList<>();
        int totalStagesCreated = 0;

        // Generate orders based on request
        if (request != null && request.getJtrcIds() != null && !request.getJtrcIds().isEmpty()) {
            // Generate orders for specific JTRCs (load with components for product auto-linking)
            for (String jtrcId : request.getJtrcIds()) {
                JewelryTechnicalReport jtrc = jtrcRepository.findActiveByIdWithComponents(jtrcId)
                        .orElseThrow(() -> new RuntimeException("JTRC not found: " + jtrcId));

                ProductionOrder order = createOrderForJtrc(plan, template, jtrc, request, userId);
                generatedOrders.add(order);
                totalStagesCreated += order.getStages().size();
            }
        } else if (request != null && request.getCollectionPlanItemIds() != null && !request.getCollectionPlanItemIds().isEmpty()) {
            // Generate orders for specific collection plan items
            for (var itemId : request.getCollectionPlanItemIds()) {
                CollectionPlanItem item = collectionPlanItemRepository.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Collection plan item not found: " + itemId));

                ProductionOrder order = createOrderForCollectionPlanItem(plan, template, item, request, userId);
                generatedOrders.add(order);
                totalStagesCreated += order.getStages().size();
            }
        } else {
            // Generate a single order for the plan
            ProductionOrder order = createSingleOrder(plan, template, request, userId);
            generatedOrders.add(order);
            totalStagesCreated += order.getStages().size();
        }

        // Update plan status to IN_PRODUCTION if it was APPROVED
        if (plan.getStatus() == ProductionPlanStatus.APPROVED) {
            plan.setStatus(ProductionPlanStatus.IN_PRODUCTION);
            plan.setActualStartDate(LocalDate.now());
            plan.setUpdatedBy(userId);
            planRepository.save(plan);
        }

        log.info("Generated {} orders with {} total stages from plan {}",
                generatedOrders.size(), totalStagesCreated, planId);

        return GenerateOrdersResponse.builder()
                .productionPlanId(plan.getId())
                .productionPlanName(plan.getName())
                .ordersGenerated(generatedOrders.size())
                .stagesCreated(totalStagesCreated)
                .orders(mapper.toListResponseList(generatedOrders))
                .message(String.format("Successfully generated %d orders with %d stages",
                        generatedOrders.size(), totalStagesCreated))
                .build();
    }

    // ==================== Helper Methods ====================

    private void createStagesFromTemplate(ProductionOrder order, WorkflowTemplate template) {
        if (template.getStages() == null || template.getStages().isEmpty()) {
            throw new IllegalStateException("Workflow template has no stages");
        }

        // Load stages if not loaded
        if (template.getStages().size() == 0) {
            template = templateRepository.findActiveByIdWithStages(template.getId())
                    .orElseThrow(() -> new RuntimeException("Template not found"));
        }

        for (WorkflowStage templateStage : template.getStages()) {
            ProductionOrderStage stage = mapper.createStageFromTemplate(templateStage, order);
            order.addStage(stage);
        }
    }

    private ProductionOrder createOrderForJtrc(ProductionPlan plan, WorkflowTemplate template,
                                               JewelryTechnicalReport jtrc, GenerateOrdersRequest request, String userId) {
        // Auto-link JTRC to MirrorProduct (match by descriptiveCode or create new)
        jtrcProductLinkService.ensureProductLinked(jtrc);

        ProductionOrder order = ProductionOrder.builder()
                .productionPlan(plan)
                .jtrc(jtrc)
                .orderNumber(generateOrderNumber())
                .quantity(1)
                .status(ProductionOrderStatus.READY)
                .currentStageOrder(1)
                .estimatedCompletionDate(request != null ? request.getEstimatedCompletionDate() : null)
                .notes(request != null ? request.getNotes() : null)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        order = orderRepository.save(order);
        createStagesFromTemplate(order, template);
        return orderRepository.save(order);
    }

    private ProductionOrder createOrderForCollectionPlanItem(ProductionPlan plan, WorkflowTemplate template,
                                                             CollectionPlanItem item, GenerateOrdersRequest request, String userId) {
        // Look up JTRC linked to this collection plan item and auto-link to product
        JewelryTechnicalReport linkedJtrc = null;
        Optional<JewelryTechnicalReport> jtrcOpt = jtrcRepository.findActiveByCollectionPlanItemId(item.getId().toString());
        if (jtrcOpt.isPresent()) {
            linkedJtrc = jtrcRepository.findActiveByIdWithComponents(jtrcOpt.get().getId()).orElse(null);
            if (linkedJtrc != null) {
                jtrcProductLinkService.ensureProductLinked(linkedJtrc);
            }
        }

        ProductionOrder order = ProductionOrder.builder()
                .productionPlan(plan)
                .collectionPlanItemId(item.getId())
                .jtrc(linkedJtrc)
                .orderNumber(generateOrderNumber())
                .quantity(item.getTargetQuantity() != null ? item.getTargetQuantity() : 1)
                .status(ProductionOrderStatus.READY)
                .currentStageOrder(1)
                .estimatedCompletionDate(request != null ? request.getEstimatedCompletionDate() : null)
                .notes(request != null ? request.getNotes() : null)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        order = orderRepository.save(order);
        createStagesFromTemplate(order, template);
        return orderRepository.save(order);
    }

    private ProductionOrder createSingleOrder(ProductionPlan plan, WorkflowTemplate template,
                                              GenerateOrdersRequest request, String userId) {
        ProductionOrder order = ProductionOrder.builder()
                .productionPlan(plan)
                .orderNumber(generateOrderNumber())
                .quantity(1)
                .status(ProductionOrderStatus.READY)
                .currentStageOrder(1)
                .estimatedCompletionDate(request != null ? request.getEstimatedCompletionDate() : null)
                .notes(request != null ? request.getNotes() : null)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        order = orderRepository.save(order);
        createStagesFromTemplate(order, template);
        return orderRepository.save(order);
    }

    private void handleStageCompletion(ProductionOrder order, ProductionOrderStage completedStage, String userId) {
        // Find next stage
        Optional<ProductionOrderStage> nextStageOpt = stageRepository.findNextStage(
                order.getId(), completedStage.getStageOrder());

        if (nextStageOpt.isPresent()) {
            // Set next stage to READY
            ProductionOrderStage nextStage = nextStageOpt.get();
            nextStage.setStatus(ProductionOrderStageStatus.READY);
            stageRepository.save(nextStage);

            // Update order current stage
            order.setCurrentStageOrder(nextStage.getStageOrder());
            order.setUpdatedBy(userId);
            orderRepository.save(order);

            // Create handoff log: component transfer from completed stage vendor to next stage (if assigned)
            if (completedStage.getAssignedVendor() != null) {
                createStageCompleteHandoff(order, completedStage, nextStage.getAssignedVendor(), userId);
            }

            log.info("Next stage {} is now READY", nextStage.getId());
        } else {
            // No more stages - order is complete
            order.setStatus(ProductionOrderStatus.COMPLETED);
            order.setActualCompletionDate(LocalDate.now());
            order.setCurrentHolderId(null); // Component returned to MIRROR
            order.setUpdatedBy(userId);
            orderRepository.save(order);

            // Create handoff log: component returned to MIRROR
            if (completedStage.getAssignedVendor() != null) {
                createReturnToMirrorHandoff(order, completedStage, userId);
            }

            log.info("Order {} is now COMPLETED", order.getId());
        }
    }

    private String generateOrderNumber() {
        // Format: PO-YYYY-MM-####
        YearMonth now = YearMonth.now();
        String prefix = String.format("PO-%d-%02d-", now.getYear(), now.getMonthValue());

        Optional<String> lastOrderNumber = orderRepository.findLastOrderNumberWithPrefix(prefix);

        int nextSequence = 1;
        if (lastOrderNumber.isPresent()) {
            String last = lastOrderNumber.get();
            String sequencePart = last.substring(prefix.length());
            try {
                nextSequence = Integer.parseInt(sequencePart) + 1;
            } catch (NumberFormatException e) {
                log.warn("Could not parse sequence from order number: {}", last);
            }
        }

        return String.format("%s%04d", prefix, nextSequence);
    }

    private void validateOrderStatusTransition(ProductionOrderStatus currentStatus, ProductionOrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        boolean isValid = switch (currentStatus) {
            case DRAFT -> newStatus == ProductionOrderStatus.READY || newStatus == ProductionOrderStatus.CANCELLED;
            case READY -> newStatus == ProductionOrderStatus.IN_PROGRESS || newStatus == ProductionOrderStatus.CANCELLED;
            case IN_PROGRESS -> newStatus == ProductionOrderStatus.COMPLETED || newStatus == ProductionOrderStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!isValid) {
            throw new IllegalStateException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }

    // ==================== Counting Methods ====================

    @Transactional(readOnly = true)
    public long countActive() {
        return orderRepository.countActive();
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return orderRepository.existsActiveById(id);
    }

    @Transactional(readOnly = true)
    public int countByProductionPlanId(String planId) {
        return orderRepository.countByProductionPlanId(planId);
    }

    // ==================== Stage Query Methods ====================

    @Transactional(readOnly = true)
    public Optional<ProductionOrderStage> findStageById(String stageId) {
        return stageRepository.findActiveById(stageId);
    }

    @Transactional(readOnly = true)
    public List<ProductionOrderStage> findStagesByOrderId(String orderId) {
        return stageRepository.findActiveByProductionOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<ProductionOrderStage> findStagesByVendorId(String vendorId) {
        return stageRepository.findActiveByAssignedVendorId(vendorId);
    }

    @Transactional(readOnly = true)
    public List<ProductionOrderStage> findStagesByVendorIdAndStatus(String vendorId, ProductionOrderStageStatus status) {
        return stageRepository.findActiveByVendorIdAndStatus(vendorId, status);
    }

    // ==================== P3-03: Bulk Partner Assignment ====================

    @Transactional
    public BulkAssignResponse bulkAssignVendors(String orderId, BulkAssignRequest request, String userId) {
        log.info("Bulk assigning vendors to order {}: {} assignments", orderId, request.getAssignments().size());

        ProductionOrder order = orderRepository.findActiveByIdWithStages(orderId)
                .orElseThrow(() -> new RuntimeException("Production order not found: " + orderId));

        List<BulkAssignResponse.AssignmentResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (StageAssignment assignment : request.getAssignments()) {
            BulkAssignResponse.AssignmentResult result = processStageAssignment(order, assignment, userId);
            results.add(result);
            if (result.isSuccess()) {
                successCount++;
            } else {
                failedCount++;
            }
        }

        // Build response
        String message = String.format("Bulk assignment completed: %d successful, %d failed out of %d total",
                successCount, failedCount, request.getAssignments().size());

        log.info("Bulk assignment result for order {}: {}", orderId, message);

        return BulkAssignResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .totalRequested(request.getAssignments().size())
                .successCount(successCount)
                .failedCount(failedCount)
                .results(results)
                .message(message)
                .build();
    }

    private BulkAssignResponse.AssignmentResult processStageAssignment(ProductionOrder order, StageAssignment assignment, String userId) {
        String stageId = assignment.getStageId();
        String vendorId = assignment.getVendorId();

        try {
            // Find the stage in the order
            ProductionOrderStage stage = order.getStages().stream()
                    .filter(s -> s.getId().equals(stageId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Stage not found in this order: " + stageId));

            // Cannot assign vendor to completed/skipped stage
            if (stage.isTerminal()) {
                throw new IllegalStateException("Cannot assign vendor to a completed or skipped stage");
            }

            // Find vendor
            Vendor vendor = vendorRepository.findById(vendorId)
                    .orElseThrow(() -> new RuntimeException("Vendor not found: " + vendorId));

            // Validate vendor has required capability for this stage
            if (stage.getRequiredCapability() != null) {
                boolean hasCapability = capabilityRepository.existsActiveByVendorIdAndCapabilityType(
                        vendor.getId(), stage.getRequiredCapability());
                if (!hasCapability) {
                    throw new IllegalArgumentException(String.format(
                            "Vendor '%s' does not have the required capability '%s'",
                            vendor.getName(), stage.getRequiredCapability()));
                }
            }

            // Update stage
            stage.setAssignedVendor(vendor);
            if (assignment.getPlannedStartDate() != null) {
                stage.setPlannedStartDate(assignment.getPlannedStartDate());
            }
            if (assignment.getPlannedEndDate() != null) {
                stage.setPlannedEndDate(assignment.getPlannedEndDate());
            }

            // Calculate estimated cost from vendor capability rates if not provided
            BigDecimal estimatedCost = assignment.getEstimatedCost();
            if (estimatedCost == null && stage.getRequiredCapability() != null) {
                estimatedCost = calculateEstimatedCostFromVendor(vendor.getId(), stage.getRequiredCapability());
            }
            if (estimatedCost != null) {
                stage.setEstimatedCost(estimatedCost);
            }

            if (assignment.getNotes() != null) {
                stage.setNotes(assignment.getNotes());
            }

            stageRepository.save(stage);

            return BulkAssignResponse.AssignmentResult.builder()
                    .stageId(stage.getId())
                    .stageName(stage.getStageName())
                    .stageOrder(stage.getStageOrder())
                    .vendorId(vendor.getId())
                    .vendorName(vendor.getName())
                    .success(true)
                    .estimatedCost(stage.getEstimatedCost())
                    .build();

        } catch (Exception e) {
            log.warn("Failed to assign vendor {} to stage {}: {}", vendorId, stageId, e.getMessage());
            return BulkAssignResponse.AssignmentResult.builder()
                    .stageId(stageId)
                    .vendorId(vendorId)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    // ==================== P3-04: Cost Estimation ====================

    @Transactional(readOnly = true)
    public CostEstimateResponse getCostEstimate(String orderId) {
        log.info("Calculating cost estimate for order: {}", orderId);

        ProductionOrder order = orderRepository.findActiveByIdWithStages(orderId)
                .orElseThrow(() -> new RuntimeException("Production order not found: " + orderId));

        List<CostEstimateResponse.StageCostBreakdown> stageBreakdowns = new ArrayList<>();
        BigDecimal totalEstimated = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        int stagesWithEstimate = 0;
        int stagesWithoutEstimate = 0;

        for (ProductionOrderStage stage : order.getStages()) {
            CostEstimateResponse.StageCostBreakdown breakdown = buildStageCostBreakdown(stage);
            stageBreakdowns.add(breakdown);

            if (breakdown.getEstimatedCost() != null) {
                totalEstimated = totalEstimated.add(breakdown.getEstimatedCost());
                stagesWithEstimate++;
            } else {
                stagesWithoutEstimate++;
            }

            if (breakdown.getActualCost() != null) {
                totalActual = totalActual.add(breakdown.getActualCost());
            }
        }

        String notes = stagesWithoutEstimate > 0
                ? String.format("%d of %d stages do not have cost estimates", stagesWithoutEstimate, order.getStages().size())
                : "All stages have cost estimates";

        return CostEstimateResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .quantity(order.getQuantity())
                .stageBreakdown(stageBreakdowns)
                .totalEstimatedCost(totalEstimated)
                .totalActualCost(totalActual.compareTo(BigDecimal.ZERO) > 0 ? totalActual : null)
                .stagesWithEstimate(stagesWithEstimate)
                .stagesWithoutEstimate(stagesWithoutEstimate)
                .totalStages(order.getStages().size())
                .currency("USD")
                .notes(notes)
                .build();
    }

    private CostEstimateResponse.StageCostBreakdown buildStageCostBreakdown(ProductionOrderStage stage) {
        CostEstimateResponse.StageCostBreakdown.StageCostBreakdownBuilder builder = CostEstimateResponse.StageCostBreakdown.builder()
                .stageId(stage.getId())
                .stageOrder(stage.getStageOrder())
                .stageName(stage.getStageName())
                .requiredCapability(stage.getRequiredCapability())
                .status(stage.getStatus())
                .estimatedCost(stage.getEstimatedCost())
                .actualCost(stage.getActualCost())
                .hasVendorAssigned(stage.hasVendorAssigned());

        if (stage.getAssignedVendor() != null) {
            Vendor vendor = stage.getAssignedVendor();
            builder.assignedVendorId(vendor.getId())
                   .assignedVendorName(vendor.getName());

            // Get vendor capability rates if available
            if (stage.getRequiredCapability() != null) {
                capabilityRepository.findActiveByVendorIdAndCapabilityType(vendor.getId(), stage.getRequiredCapability())
                        .ifPresent(cap -> {
                            builder.vendorCostPerPiece(cap.getCostPerPiece())
                                   .vendorCostPerGram(cap.getCostPerGram());
                        });
            }

            // Determine cost source
            if (stage.getEstimatedCost() != null) {
                builder.costSource("MANUAL");
            } else {
                builder.costSource("VENDOR_RATE");
            }
        } else {
            builder.costSource("NOT_ASSIGNED");
        }

        return builder.build();
    }

    private BigDecimal calculateEstimatedCostFromVendor(String vendorId, com.mirror.product.enums.PartnerCapabilityType capabilityType) {
        return capabilityRepository.findActiveByVendorIdAndCapabilityType(vendorId, capabilityType)
                .map(PartnerCapability::getCostPerPiece)
                .orElse(null);
    }

    // ==================== P3-10: Find Orders by JTRC ====================

    @Transactional(readOnly = true)
    public List<ProductionOrder> findByJtrcId(String jtrcId) {
        return orderRepository.findActiveByJtrcId(jtrcId);
    }

    // ==================== Component Ownership Tracking ====================

    /**
     * Create handoff log when a stage is started (component transfer to assigned vendor)
     */
    private void createStageStartHandoff(ProductionOrder order, ProductionOrderStage stage, String userId) {
        // Find the previous holder (from currentHolderId or null if from MIRROR)
        String previousHolderId = null;

        // Get the previous completed stage to find the "from" vendor
        Optional<ProductionOrderStage> previousStage = stageRepository.findPreviousStage(order.getId(), stage.getStageOrder());
        if (previousStage.isPresent() && previousStage.get().getAssignedVendor() != null) {
            previousHolderId = previousStage.get().getAssignedVendor().getId();
        }

        Vendor fromVendor = previousHolderId != null ? vendorRepository.findById(previousHolderId).orElse(null) : null;
        Vendor toVendor = stage.getAssignedVendor();

        HandoffType handoffType = fromVendor == null ? HandoffType.INITIAL_ASSIGNMENT : HandoffType.STAGE_START;

        ComponentOwnershipLog handoffLog = ComponentOwnershipLog.builder()
                .productionOrder(order)
                .stage(stage)
                .fromVendor(fromVendor)
                .toVendor(toVendor)
                .handoffType(handoffType)
                .status(HandoffStatus.INITIATED)
                .initiatedBy(userId)
                .initiatedAt(LocalDateTime.now())
                .reason("Stage started: " + stage.getStageName())
                .build();

        ownershipLogRepository.save(handoffLog);
        log.info("Created {} handoff log for stage {} from {} to {}",
                handoffType, stage.getId(),
                fromVendor != null ? fromVendor.getId() : "MIRROR",
                toVendor.getId());
    }

    /**
     * Create handoff log when a stage is completed (component transfer to next vendor or back to MIRROR)
     */
    private void createStageCompleteHandoff(ProductionOrder order, ProductionOrderStage completedStage,
                                            Vendor nextVendor, String userId) {
        Vendor fromVendor = completedStage.getAssignedVendor();

        HandoffType handoffType = nextVendor != null ? HandoffType.STAGE_COMPLETE : HandoffType.RETURN_TO_MIRROR;

        ComponentOwnershipLog handoffLog = ComponentOwnershipLog.builder()
                .productionOrder(order)
                .stage(completedStage)
                .fromVendor(fromVendor)
                .toVendor(nextVendor)
                .handoffType(handoffType)
                .status(HandoffStatus.INITIATED)
                .initiatedBy(userId)
                .initiatedAt(LocalDateTime.now())
                .reason("Stage completed: " + completedStage.getStageName())
                .build();

        ownershipLogRepository.save(handoffLog);
        log.info("Created {} handoff log for completed stage {} from {} to {}",
                handoffType, completedStage.getId(),
                fromVendor != null ? fromVendor.getId() : "UNKNOWN",
                nextVendor != null ? nextVendor.getId() : "MIRROR");
    }

    /**
     * Create handoff log when order is completed and component returns to MIRROR
     */
    private void createReturnToMirrorHandoff(ProductionOrder order, ProductionOrderStage finalStage, String userId) {
        Vendor fromVendor = finalStage.getAssignedVendor();

        ComponentOwnershipLog handoffLog = ComponentOwnershipLog.builder()
                .productionOrder(order)
                .stage(finalStage)
                .fromVendor(fromVendor)
                .toVendor(null) // Back to MIRROR
                .handoffType(HandoffType.RETURN_TO_MIRROR)
                .status(HandoffStatus.INITIATED)
                .initiatedBy(userId)
                .initiatedAt(LocalDateTime.now())
                .reason("Production completed - component returned to MIRROR")
                .build();

        ownershipLogRepository.save(handoffLog);
        log.info("Created RETURN_TO_MIRROR handoff log for completed order {} from {}",
                order.getId(), fromVendor != null ? fromVendor.getId() : "UNKNOWN");
    }
}
