package com.mirror.product.mapper;

import com.mirror.product.dto.workflow.*;
import com.mirror.product.entity.CollectionPlan;
import com.mirror.product.entity.ProductionPlan;
import com.mirror.product.entity.WorkflowTemplate;
import com.mirror.product.enums.ProductionPlanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for ProductionPlan entities and DTOs
 */
@Component
@RequiredArgsConstructor
public class ProductionPlanMapper {

    private final WorkflowTemplateMapper workflowTemplateMapper;

    public ProductionPlanResponse toResponse(ProductionPlan entity) {
        if (entity == null) {
            return null;
        }
        return ProductionPlanResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .collectionPlanId(entity.getCollectionPlanId())
                .workflowTemplateId(entity.getWorkflowTemplate() != null ? entity.getWorkflowTemplate().getId() : null)
                .workflowTemplateName(entity.getWorkflowTemplate() != null ? entity.getWorkflowTemplate().getName() : null)
                .targetStartDate(entity.getTargetStartDate())
                .targetEndDate(entity.getTargetEndDate())
                .actualStartDate(entity.getActualStartDate())
                .actualEndDate(entity.getActualEndDate())
                .notes(entity.getNotes())
                .isEditable(entity.isEditable())
                .isCancellable(entity.isCancellable())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isActive(entity.getIsActive())
                .build();
    }

    public ProductionPlanResponse toResponseWithTemplate(ProductionPlan entity) {
        if (entity == null) {
            return null;
        }
        ProductionPlanResponse response = toResponse(entity);
        if (entity.getWorkflowTemplate() != null) {
            response.setWorkflowTemplate(workflowTemplateMapper.toResponse(entity.getWorkflowTemplate()));
        }
        return response;
    }

    public ProductionPlanResponse toResponseWithCollectionPlan(ProductionPlan entity, CollectionPlan collectionPlan) {
        if (entity == null) {
            return null;
        }
        ProductionPlanResponse response = toResponse(entity);
        if (collectionPlan != null) {
            response.setCollectionPlanName(collectionPlan.getName());
        }
        return response;
    }

    public ProductionPlanListResponse toListResponse(ProductionPlan entity) {
        if (entity == null) {
            return null;
        }
        return ProductionPlanListResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .collectionPlanId(entity.getCollectionPlanId())
                .workflowTemplateId(entity.getWorkflowTemplate() != null ? entity.getWorkflowTemplate().getId() : null)
                .workflowTemplateName(entity.getWorkflowTemplate() != null ? entity.getWorkflowTemplate().getName() : null)
                .targetStartDate(entity.getTargetStartDate())
                .targetEndDate(entity.getTargetEndDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ProductionPlanListResponse toListResponseWithCollectionPlan(ProductionPlan entity, CollectionPlan collectionPlan) {
        if (entity == null) {
            return null;
        }
        ProductionPlanListResponse response = toListResponse(entity);
        if (collectionPlan != null) {
            response.setCollectionPlanName(collectionPlan.getName());
        }
        return response;
    }

    public ProductionPlan toEntity(ProductionPlanCreateRequest request, WorkflowTemplate workflowTemplate) {
        if (request == null) {
            return null;
        }
        return ProductionPlan.builder()
                .name(request.getName())
                .collectionPlanId(request.getCollectionPlanId())
                .workflowTemplate(workflowTemplate)
                .status(ProductionPlanStatus.DRAFT)
                .targetStartDate(request.getTargetStartDate())
                .targetEndDate(request.getTargetEndDate())
                .notes(request.getNotes())
                .build();
    }

    public ProductionPlan toEntityFromCollectionPlan(
            CreateFromCollectionPlanRequest request,
            CollectionPlan collectionPlan,
            WorkflowTemplate workflowTemplate) {
        if (request == null || collectionPlan == null) {
            return null;
        }

        String planName = request.getName();
        if (planName == null || planName.isBlank()) {
            // Generate name from collection plan
            planName = "Production Plan - " + collectionPlan.getName();
        }

        return ProductionPlan.builder()
                .name(planName)
                .collectionPlanId(collectionPlan.getId())
                .workflowTemplate(workflowTemplate)
                .status(ProductionPlanStatus.DRAFT)
                .targetStartDate(request.getTargetStartDate() != null
                        ? request.getTargetStartDate()
                        : collectionPlan.getDeadline() != null
                                ? collectionPlan.getDeadline().minusDays(30) // Start 30 days before deadline
                                : null)
                .targetEndDate(request.getTargetEndDate() != null
                        ? request.getTargetEndDate()
                        : collectionPlan.getDeadline())
                .notes(request.getNotes())
                .build();
    }

    public void updateEntity(ProductionPlan entity, ProductionPlanUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getTargetStartDate() != null) {
            entity.setTargetStartDate(request.getTargetStartDate());
        }
        if (request.getTargetEndDate() != null) {
            entity.setTargetEndDate(request.getTargetEndDate());
        }
        if (request.getActualStartDate() != null) {
            entity.setActualStartDate(request.getActualStartDate());
        }
        if (request.getActualEndDate() != null) {
            entity.setActualEndDate(request.getActualEndDate());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }
    }

    public List<ProductionPlanResponse> toResponseList(List<ProductionPlan> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductionPlanListResponse> toListResponseList(List<ProductionPlan> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }
}
