package com.mirror.product.mapper;

import com.mirror.product.dto.collectionplan.*;
import com.mirror.product.entity.CollectionPlan;
import com.mirror.product.entity.CollectionPlanItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CollectionPlanMapper {

    public CollectionPlanResponse toResponse(CollectionPlan entity) {
        if (entity == null) return null;

        List<CollectionPlanItemResponse> itemResponses = new ArrayList<>();
        if (entity.getItems() != null) {
            itemResponses = entity.getItems().stream()
                    .map(this::toItemResponse)
                    .collect(Collectors.toList());
        }

        return CollectionPlanResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .totalQuantity(entity.getTotalQuantity())
                .deadline(entity.getDeadline())
                .status(entity.getStatus())
                .totalCost(entity.getTotalCost())
                .estimatedDeliveryDate(entity.getEstimatedDeliveryDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .items(itemResponses)
                .itemCount(itemResponses.size())
                .build();
    }

    public CollectionPlanResponse toListResponse(CollectionPlan entity) {
        if (entity == null) return null;

        int itemCount = entity.getItems() != null ? entity.getItems().size() : 0;

        return CollectionPlanResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .totalQuantity(entity.getTotalQuantity())
                .deadline(entity.getDeadline())
                .status(entity.getStatus())
                .totalCost(entity.getTotalCost())
                .estimatedDeliveryDate(entity.getEstimatedDeliveryDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .itemCount(itemCount)
                .build();
    }

    public CollectionPlan toEntity(CollectionPlanCreateRequest request) {
        if (request == null) return null;

        return CollectionPlan.builder()
                .name(request.getName())
                .totalQuantity(request.getTotalQuantity() != null ? request.getTotalQuantity() : 0)
                .deadline(request.getDeadline())
                .estimatedDeliveryDate(request.getEstimatedDeliveryDate())
                .status("DRAFT")
                .build();
    }

    public void updateEntity(CollectionPlan entity, CollectionPlanUpdateRequest request) {
        if (entity == null || request == null) return;

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getTotalQuantity() != null) {
            entity.setTotalQuantity(request.getTotalQuantity());
        }
        if (request.getDeadline() != null) {
            entity.setDeadline(request.getDeadline());
        }
        if (request.getEstimatedDeliveryDate() != null) {
            entity.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        }
    }

    public CollectionPlanItemResponse toItemResponse(CollectionPlanItem item) {
        if (item == null) return null;

        BigDecimal subtotal = null;
        if (item.getTargetQuantity() != null && item.getEstimatedUnitCost() != null) {
            subtotal = item.getEstimatedUnitCost().multiply(BigDecimal.valueOf(item.getTargetQuantity()));
        }

        return CollectionPlanItemResponse.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .productType(item.getProductType())
                .baseDesign(item.getBaseDesign())
                .targetQuantity(item.getTargetQuantity())
                .estimatedUnitCost(item.getEstimatedUnitCost())
                .subtotal(subtotal)
                .jewelrySpecId(item.getJewelrySpecId())
                .build();
    }

    public CollectionPlanItem toItemEntity(CollectionPlanItemRequest request, CollectionPlan plan) {
        if (request == null) return null;

        return CollectionPlanItem.builder()
                .collectionPlan(plan)
                .productName(request.getProductName())
                .productType(request.getProductType())
                .baseDesign(request.getBaseDesign())
                .targetQuantity(request.getTargetQuantity())
                .estimatedUnitCost(request.getEstimatedUnitCost())
                .build();
    }
}
