package com.mirror.product.mapper;

import com.mirror.product.dto.workflow.*;
import com.mirror.product.entity.WorkflowStage;
import com.mirror.product.entity.WorkflowTemplate;
import com.mirror.product.enums.WorkflowTemplateStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for WorkflowTemplate and WorkflowStage entities and DTOs
 */
@Component
public class WorkflowTemplateMapper {

    // ==================== Stage Mapping ====================

    public WorkflowStageDTO toStageDTO(WorkflowStage entity) {
        if (entity == null) {
            return null;
        }
        return WorkflowStageDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .stageOrder(entity.getStageOrder())
                .requiredCapability(entity.getRequiredCapability())
                .estimatedDurationDays(entity.getEstimatedDurationDays())
                .instructions(entity.getInstructions())
                .isFinalStage(entity.getIsFinalStage())
                .build();
    }

    public WorkflowStage toStageEntity(WorkflowStageDTO dto) {
        if (dto == null) {
            return null;
        }
        return WorkflowStage.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .stageOrder(dto.getStageOrder())
                .requiredCapability(dto.getRequiredCapability())
                .estimatedDurationDays(dto.getEstimatedDurationDays())
                .instructions(dto.getInstructions())
                .isFinalStage(dto.getIsFinalStage() != null ? dto.getIsFinalStage() : false)
                .build();
    }

    public void updateStageEntity(WorkflowStage entity, WorkflowStageDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getStageOrder() != null) {
            entity.setStageOrder(dto.getStageOrder());
        }
        if (dto.getRequiredCapability() != null) {
            entity.setRequiredCapability(dto.getRequiredCapability());
        }
        if (dto.getEstimatedDurationDays() != null) {
            entity.setEstimatedDurationDays(dto.getEstimatedDurationDays());
        }
        if (dto.getInstructions() != null) {
            entity.setInstructions(dto.getInstructions());
        }
        if (dto.getIsFinalStage() != null) {
            entity.setIsFinalStage(dto.getIsFinalStage());
        }
    }

    public List<WorkflowStageDTO> toStageDTOList(List<WorkflowStage> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toStageDTO)
                .collect(Collectors.toList());
    }

    public List<WorkflowStage> toStageEntityList(List<WorkflowStageDTO> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .map(this::toStageEntity)
                .collect(Collectors.toList());
    }

    // ==================== Template Mapping ====================

    public WorkflowTemplateResponse toResponse(WorkflowTemplate entity) {
        if (entity == null) {
            return null;
        }
        return WorkflowTemplateResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .isDefault(entity.getIsDefault())
                .status(entity.getStatus())
                .stages(toStageDTOList(entity.getStages()))
                .stageCount(entity.getStageCount())
                .totalEstimatedDurationDays(entity.getTotalEstimatedDurationDays())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isActive(entity.getIsActive())
                .build();
    }

    public WorkflowTemplateListResponse toListResponse(WorkflowTemplate entity) {
        if (entity == null) {
            return null;
        }
        return WorkflowTemplateListResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .isDefault(entity.getIsDefault())
                .status(entity.getStatus())
                .stageCount(entity.getStageCount())
                .totalEstimatedDurationDays(entity.getTotalEstimatedDurationDays())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public WorkflowTemplate toEntity(WorkflowTemplateCreateRequest request) {
        if (request == null) {
            return null;
        }
        WorkflowTemplate template = WorkflowTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .status(WorkflowTemplateStatus.DRAFT)
                .stages(new ArrayList<>())
                .build();

        // Add stages
        if (request.getStages() != null) {
            for (WorkflowStageDTO stageDTO : request.getStages()) {
                WorkflowStage stage = toStageEntity(stageDTO);
                template.addStage(stage);
            }
        }

        return template;
    }

    public void updateEntity(WorkflowTemplate entity, WorkflowTemplateUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory());
        }
        if (request.getIsDefault() != null) {
            entity.setIsDefault(request.getIsDefault());
        }
    }

    public List<WorkflowTemplateResponse> toResponseList(List<WorkflowTemplate> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<WorkflowTemplateListResponse> toListResponseList(List<WorkflowTemplate> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a duplicate of a template (for clone operation)
     */
    public WorkflowTemplate createDuplicate(WorkflowTemplate source, String newName) {
        if (source == null) {
            return null;
        }

        WorkflowTemplate duplicate = WorkflowTemplate.builder()
                .name(newName)
                .description(source.getDescription())
                .category(source.getCategory())
                .isDefault(false) // Duplicates are never default
                .status(WorkflowTemplateStatus.DRAFT) // Duplicates start as DRAFT
                .stages(new ArrayList<>())
                .build();

        // Duplicate stages
        if (source.getStages() != null) {
            for (WorkflowStage sourceStage : source.getStages()) {
                WorkflowStage duplicateStage = WorkflowStage.builder()
                        .name(sourceStage.getName())
                        .description(sourceStage.getDescription())
                        .stageOrder(sourceStage.getStageOrder())
                        .requiredCapability(sourceStage.getRequiredCapability())
                        .estimatedDurationDays(sourceStage.getEstimatedDurationDays())
                        .instructions(sourceStage.getInstructions())
                        .isFinalStage(sourceStage.getIsFinalStage())
                        .build();
                duplicate.addStage(duplicateStage);
            }
        }

        return duplicate;
    }
}
