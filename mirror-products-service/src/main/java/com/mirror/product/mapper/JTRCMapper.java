package com.mirror.product.mapper;

import com.mirror.product.dto.jtrc.*;
import com.mirror.product.entity.JewelryTechnicalReport;
import com.mirror.product.entity.JTRCLaborComponent;
import com.mirror.product.entity.JTRCMetalComponent;
import com.mirror.product.entity.JTRCStoneComponent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for JTRC (Jewelry Technical Report Card) entities and DTOs
 */
@Component
public class JTRCMapper {

    // ==================== Metal Component Mapping ====================

    public JTRCMetalComponentDTO toMetalComponentDTO(JTRCMetalComponent entity) {
        if (entity == null) {
            return null;
        }
        return JTRCMetalComponentDTO.builder()
                .id(entity.getId())
                .metalType(entity.getMetalType())
                .metalPurity(entity.getMetalPurity())
                .weightGrams(entity.getWeightGrams())
                .lossRatePercent(entity.getLossRatePercent())
                .pricePerGram(entity.getPricePerGram())
                .metalCost(entity.getMetalCost())
                .lossCost(entity.getLossCost())
                .totalCost(entity.getTotalCost())
                .build();
    }

    public JTRCMetalComponent toMetalComponentEntity(JTRCMetalComponentDTO dto) {
        if (dto == null) {
            return null;
        }
        return JTRCMetalComponent.builder()
                .metalType(dto.getMetalType())
                .metalPurity(dto.getMetalPurity())
                .weightGrams(dto.getWeightGrams())
                .lossRatePercent(dto.getLossRatePercent())
                .pricePerGram(dto.getPricePerGram())
                .build();
    }

    public void updateMetalComponentEntity(JTRCMetalComponent entity, JTRCMetalComponentDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        if (dto.getMetalType() != null) {
            entity.setMetalType(dto.getMetalType());
        }
        if (dto.getMetalPurity() != null) {
            entity.setMetalPurity(dto.getMetalPurity());
        }
        if (dto.getWeightGrams() != null) {
            entity.setWeightGrams(dto.getWeightGrams());
        }
        if (dto.getLossRatePercent() != null) {
            entity.setLossRatePercent(dto.getLossRatePercent());
        }
        if (dto.getPricePerGram() != null) {
            entity.setPricePerGram(dto.getPricePerGram());
        }
    }

    // ==================== Stone Component Mapping ====================

    public JTRCStoneComponentDTO toStoneComponentDTO(JTRCStoneComponent entity) {
        if (entity == null) {
            return null;
        }
        return JTRCStoneComponentDTO.builder()
                .id(entity.getId())
                .stoneRole(entity.getStoneRole())
                .stoneType(entity.getStoneType())
                .stoneDetail(entity.getStoneDetail())
                .shape(entity.getShape())
                .colorCategory(entity.getColorCategory())
                .colorGrade(entity.getColorGrade())
                .colorIntensity(entity.getColorIntensity())
                .colorName(entity.getColorName())
                .clarity(entity.getClarity())
                .sizeMm(entity.getSizeMm())
                .weightCarat(entity.getWeightCarat())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .totalPrice(entity.getTotalPrice())
                .notes(entity.getNotes())
                .build();
    }

    public JTRCStoneComponent toStoneComponentEntity(JTRCStoneComponentDTO dto) {
        if (dto == null) {
            return null;
        }
        return JTRCStoneComponent.builder()
                .stoneRole(dto.getStoneRole())
                .stoneType(dto.getStoneType())
                .stoneDetail(dto.getStoneDetail())
                .shape(dto.getShape())
                .colorCategory(dto.getColorCategory())
                .colorGrade(dto.getColorGrade())
                .colorIntensity(dto.getColorIntensity())
                .colorName(dto.getColorName())
                .clarity(dto.getClarity())
                .sizeMm(dto.getSizeMm())
                .weightCarat(dto.getWeightCarat())
                .quantity(dto.getQuantity() != null ? dto.getQuantity() : 1)
                .unitPrice(dto.getUnitPrice())
                .notes(dto.getNotes())
                .build();
    }

    public List<JTRCStoneComponentDTO> toStoneComponentDTOList(Collection<JTRCStoneComponent> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toStoneComponentDTO)
                .collect(Collectors.toList());
    }

    public List<JTRCStoneComponent> toStoneComponentEntityList(List<JTRCStoneComponentDTO> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .map(this::toStoneComponentEntity)
                .collect(Collectors.toList());
    }

    // ==================== Labor Component Mapping ====================

    public JTRCLaborComponentDTO toLaborComponentDTO(JTRCLaborComponent entity) {
        if (entity == null) {
            return null;
        }
        return JTRCLaborComponentDTO.builder()
                .id(entity.getId())
                .laborType(entity.getLaborType())
                .description(entity.getDescription())
                .cost(entity.getCost())
                .notes(entity.getNotes())
                .build();
    }

    public JTRCLaborComponent toLaborComponentEntity(JTRCLaborComponentDTO dto) {
        if (dto == null) {
            return null;
        }
        return JTRCLaborComponent.builder()
                .laborType(dto.getLaborType())
                .description(dto.getDescription())
                .cost(dto.getCost())
                .notes(dto.getNotes())
                .build();
    }

    public List<JTRCLaborComponentDTO> toLaborComponentDTOList(Collection<JTRCLaborComponent> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toLaborComponentDTO)
                .collect(Collectors.toList());
    }

    public List<JTRCLaborComponent> toLaborComponentEntityList(List<JTRCLaborComponentDTO> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .map(this::toLaborComponentEntity)
                .collect(Collectors.toList());
    }

    // ==================== JTRC Main Entity Mapping ====================

    public JTRCResponse toResponse(JewelryTechnicalReport entity) {
        if (entity == null) {
            return null;
        }
        return JTRCResponse.builder()
                .id(entity.getId())
                .reportNumber(entity.getReportNumber())
                .collectionPlanItemId(entity.getCollectionPlanItemId())
                .productId(entity.getProductId())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .collection(entity.getCollection())
                .season(entity.getSeason())
                .projectId(entity.getProjectId())
                .category(entity.getCategory())
                .source(entity.getSource())
                .entryDate(entity.getEntryDate())
                .goldPricePerGram(entity.getGoldPricePerGram())
                .exchangeRateUsd(entity.getExchangeRateUsd())
                .totalMetalCost(entity.getTotalMetalCost())
                .totalStoneCost(entity.getTotalStoneCost())
                .totalLaborCost(entity.getTotalLaborCost())
                .totalCogsVnd(entity.getTotalCogsVnd())
                .totalCogsUsd(entity.getTotalCogsUsd())
                .productionDifficulty(entity.getProductionDifficulty())
                .estimatedLeadTimeDays(entity.getEstimatedLeadTimeDays())
                .castingStatus(entity.getCastingStatus())
                .productionNotes(entity.getProductionNotes())
                .render3dUrl(entity.getRender3dUrl())
                .stoneMapUrl(entity.getStoneMapUrl())
                .technicalDrawingUrl(entity.getTechnicalDrawingUrl())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isActive(entity.getIsActive())
                .metalComponent(toMetalComponentDTO(entity.getMetalComponent()))
                .stoneComponents(toStoneComponentDTOList(entity.getStoneComponents()))
                .laborComponents(toLaborComponentDTOList(entity.getLaborComponents()))
                .build();
    }

    public JTRCListResponse toListResponse(JewelryTechnicalReport entity) {
        if (entity == null) {
            return null;
        }
        return JTRCListResponse.builder()
                .id(entity.getId())
                .reportNumber(entity.getReportNumber())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .collection(entity.getCollection())
                .season(entity.getSeason())
                .projectId(entity.getProjectId())
                .category(entity.getCategory())
                .entryDate(entity.getEntryDate())
                .totalCogsVnd(entity.getTotalCogsVnd())
                .totalCogsUsd(entity.getTotalCogsUsd())
                .productionDifficulty(entity.getProductionDifficulty())
                .estimatedLeadTimeDays(entity.getEstimatedLeadTimeDays())
                .stoneComponentCount(entity.getStoneComponents() != null ? entity.getStoneComponents().size() : 0)
                .laborComponentCount(entity.getLaborComponents() != null ? entity.getLaborComponents().size() : 0)
                .hasMetalComponent(entity.getMetalComponent() != null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public JewelryTechnicalReport toEntity(JTRCCreateRequest request) {
        if (request == null) {
            return null;
        }
        return JewelryTechnicalReport.builder()
                .collectionPlanItemId(request.getCollectionPlanItemId())
                .productId(request.getProductId())
                .collection(request.getCollection())
                .season(request.getSeason())
                .projectId(request.getProjectId())
                .category(request.getCategory())
                .source(request.getSource())
                .entryDate(request.getEntryDate())
                .goldPricePerGram(request.getGoldPricePerGram())
                .exchangeRateUsd(request.getExchangeRateUsd())
                .productionDifficulty(request.getProductionDifficulty())
                .estimatedLeadTimeDays(request.getEstimatedLeadTimeDays())
                .castingStatus(request.getCastingStatus())
                .productionNotes(request.getProductionNotes())
                .render3dUrl(request.getRender3dUrl())
                .stoneMapUrl(request.getStoneMapUrl())
                .technicalDrawingUrl(request.getTechnicalDrawingUrl())
                .build();
    }

    public void updateEntity(JewelryTechnicalReport entity, JTRCUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getCollectionPlanItemId() != null) {
            entity.setCollectionPlanItemId(request.getCollectionPlanItemId());
        }
        if (request.getProductId() != null) {
            entity.setProductId(request.getProductId());
        }
        if (request.getCollection() != null) {
            entity.setCollection(request.getCollection());
        }
        if (request.getSeason() != null) {
            entity.setSeason(request.getSeason());
        }
        if (request.getProjectId() != null) {
            entity.setProjectId(request.getProjectId());
        }
        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory());
        }
        if (request.getSource() != null) {
            entity.setSource(request.getSource());
        }
        if (request.getEntryDate() != null) {
            entity.setEntryDate(request.getEntryDate());
        }
        if (request.getGoldPricePerGram() != null) {
            entity.setGoldPricePerGram(request.getGoldPricePerGram());
        }
        if (request.getExchangeRateUsd() != null) {
            entity.setExchangeRateUsd(request.getExchangeRateUsd());
        }
        if (request.getProductionDifficulty() != null) {
            entity.setProductionDifficulty(request.getProductionDifficulty());
        }
        if (request.getEstimatedLeadTimeDays() != null) {
            entity.setEstimatedLeadTimeDays(request.getEstimatedLeadTimeDays());
        }
        if (request.getCastingStatus() != null) {
            entity.setCastingStatus(request.getCastingStatus());
        }
        if (request.getProductionNotes() != null) {
            entity.setProductionNotes(request.getProductionNotes());
        }
        if (request.getRender3dUrl() != null) {
            entity.setRender3dUrl(request.getRender3dUrl());
        }
        if (request.getStoneMapUrl() != null) {
            entity.setStoneMapUrl(request.getStoneMapUrl());
        }
        if (request.getTechnicalDrawingUrl() != null) {
            entity.setTechnicalDrawingUrl(request.getTechnicalDrawingUrl());
        }
    }

    public List<JTRCResponse> toResponseList(List<JewelryTechnicalReport> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<JTRCListResponse> toListResponseList(List<JewelryTechnicalReport> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }
}
