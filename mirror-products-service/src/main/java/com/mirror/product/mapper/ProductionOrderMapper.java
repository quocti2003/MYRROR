package com.mirror.product.mapper;

import com.mirror.product.dto.productionorder.*;
import com.mirror.product.entity.*;
import com.mirror.product.enums.ProductionOrderStageStatus;
import com.mirror.product.enums.ProductionOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for ProductionOrder and ProductionOrderStage entities and DTOs
 */
@Component
@RequiredArgsConstructor
public class ProductionOrderMapper {

    // ==================== Production Order Mappings ====================

    public ProductionOrderResponse toResponse(ProductionOrder entity) {
        if (entity == null) {
            return null;
        }

        ProductionOrderResponse response = ProductionOrderResponse.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .status(entity.getStatus())
                .productionPlanId(entity.getProductionPlan() != null ? entity.getProductionPlan().getId() : null)
                .productionPlanName(entity.getProductionPlan() != null ? entity.getProductionPlan().getName() : null)
                .collectionPlanItemId(entity.getCollectionPlanItemId())
                .jtrcId(entity.getJtrc() != null ? entity.getJtrc().getId() : null)
                .jtrcReportNumber(entity.getJtrc() != null ? entity.getJtrc().getReportNumber() : null)
                .quantity(entity.getQuantity())
                .currentStageOrder(entity.getCurrentStageOrder())
                .currentHolderId(entity.getCurrentHolderId())
                .estimatedCompletionDate(entity.getEstimatedCompletionDate())
                .actualCompletionDate(entity.getActualCompletionDate())
                .notes(entity.getNotes())
                .needsSpecReview(entity.getNeedsSpecReview())
                .totalStages(entity.getTotalStageCount())
                .completedStages(entity.getCompletedStageCount())
                .progressPercentage(entity.getProgressPercentage())
                .isEditable(entity.isEditable())
                .isCancellable(entity.isCancellable())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isActive(entity.getIsActive())
                .build();

        // Set current stage name
        if (entity.getCurrentStage() != null) {
            response.setCurrentStageName(entity.getCurrentStage().getStageName());
        }

        return response;
    }

    public ProductionOrderResponse toResponseWithStages(ProductionOrder entity) {
        if (entity == null) {
            return null;
        }

        ProductionOrderResponse response = toResponse(entity);

        if (entity.getStages() != null) {
            response.setStages(entity.getStages().stream()
                    .map(this::toStageDTO)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    public ProductionOrderListResponse toListResponse(ProductionOrder entity) {
        if (entity == null) {
            return null;
        }

        ProductionOrderListResponse response = ProductionOrderListResponse.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .status(entity.getStatus())
                .productionPlanId(entity.getProductionPlan() != null ? entity.getProductionPlan().getId() : null)
                .productionPlanName(entity.getProductionPlan() != null ? entity.getProductionPlan().getName() : null)
                .collectionPlanItemId(entity.getCollectionPlanItemId())
                .jtrcId(entity.getJtrc() != null ? entity.getJtrc().getId() : null)
                .quantity(entity.getQuantity())
                .currentStageOrder(entity.getCurrentStageOrder())
                .currentHolderId(entity.getCurrentHolderId())
                .estimatedCompletionDate(entity.getEstimatedCompletionDate())
                .actualCompletionDate(entity.getActualCompletionDate())
                .needsSpecReview(entity.getNeedsSpecReview())
                .totalStages(entity.getTotalStageCount())
                .completedStages(entity.getCompletedStageCount())
                .progressPercentage(entity.getProgressPercentage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // Set current stage name
        if (entity.getCurrentStage() != null) {
            response.setCurrentStageName(entity.getCurrentStage().getStageName());
        }

        return response;
    }

    public ProductionOrder toEntity(ProductionOrderCreateRequest request, ProductionPlan plan) {
        if (request == null || plan == null) {
            return null;
        }

        return ProductionOrder.builder()
                .productionPlan(plan)
                .collectionPlanItemId(request.getCollectionPlanItemId())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .status(ProductionOrderStatus.DRAFT)
                .estimatedCompletionDate(request.getEstimatedCompletionDate())
                .notes(request.getNotes())
                .build();
    }

    public void updateEntity(ProductionOrder entity, ProductionOrderUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getCollectionPlanItemId() != null) {
            entity.setCollectionPlanItemId(request.getCollectionPlanItemId());
        }
        if (request.getQuantity() != null) {
            entity.setQuantity(request.getQuantity());
        }
        if (request.getEstimatedCompletionDate() != null) {
            entity.setEstimatedCompletionDate(request.getEstimatedCompletionDate());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }
    }

    public List<ProductionOrderResponse> toResponseList(List<ProductionOrder> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductionOrderListResponse> toListResponseList(List<ProductionOrder> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }

    // ==================== Production Order Stage Mappings ====================

    public ProductionOrderStageDTO toStageDTO(ProductionOrderStage entity) {
        if (entity == null) {
            return null;
        }

        return ProductionOrderStageDTO.builder()
                .id(entity.getId())
                .productionOrderId(entity.getProductionOrder() != null ? entity.getProductionOrder().getId() : null)
                .workflowStageId(entity.getWorkflowStage() != null ? entity.getWorkflowStage().getId() : null)
                .stageOrder(entity.getStageOrder())
                .stageName(entity.getStageName())
                .requiredCapability(entity.getRequiredCapability())
                .assignedVendorId(entity.getAssignedVendor() != null ? entity.getAssignedVendor().getId() : null)
                .assignedVendorName(entity.getAssignedVendor() != null ? entity.getAssignedVendor().getName() : null)
                .status(entity.getStatus())
                .plannedStartDate(entity.getPlannedStartDate())
                .plannedEndDate(entity.getPlannedEndDate())
                .actualStartDate(entity.getActualStartDate())
                .actualEndDate(entity.getActualEndDate())
                .estimatedCost(entity.getEstimatedCost())
                .actualCost(entity.getActualCost())
                .isFinalStage(entity.getIsFinalStage())
                .notes(entity.getNotes())
                .completedBy(entity.getCompletedBy())
                .completedAt(entity.getCompletedAt())
                .canStart(entity.canStart())
                .canComplete(entity.canComplete())
                .canSkip(entity.canSkip())
                .canAssign(entity.canAssign())
                .isTerminal(entity.isTerminal())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public List<ProductionOrderStageDTO> toStageDTOList(List<ProductionOrderStage> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toStageDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a ProductionOrderStage from a WorkflowStage template
     */
    public ProductionOrderStage createStageFromTemplate(WorkflowStage templateStage, ProductionOrder order) {
        if (templateStage == null) {
            return null;
        }

        // First stage is READY, others are BLOCKED
        ProductionOrderStageStatus initialStatus = templateStage.getStageOrder() == 1
                ? ProductionOrderStageStatus.READY
                : ProductionOrderStageStatus.BLOCKED;

        return ProductionOrderStage.builder()
                .productionOrder(order)
                .workflowStage(templateStage)
                .stageOrder(templateStage.getStageOrder())
                .stageName(templateStage.getName())
                .requiredCapability(templateStage.getRequiredCapability())
                .status(initialStatus)
                .isFinalStage(templateStage.getIsFinalStage())
                .build();
    }
}
