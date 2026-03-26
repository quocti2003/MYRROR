package com.mirror.product.service;

import com.mirror.product.dto.collectionplan.*;
import com.mirror.product.entity.CollectionPlan;
import com.mirror.product.entity.CollectionPlanItem;
import com.mirror.product.mapper.CollectionPlanMapper;
import com.mirror.product.repository.CollectionPlanItemRepository;
import com.mirror.product.repository.CollectionPlanRepository;
import com.mirror.product.repository.ProductionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionPlanService {

    private final CollectionPlanRepository planRepository;
    private final CollectionPlanItemRepository itemRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final CollectionPlanMapper mapper;

    // ==================== Find Operations ====================

    @Transactional(readOnly = true)
    public Page<CollectionPlan> findAll(String search, String status, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return planRepository.searchByName(search.trim(), status, pageable);
        }
        return planRepository.findAllWithFilters(status, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<CollectionPlan> findByIdWithItems(UUID id) {
        return planRepository.findByIdWithItems(id);
    }

    @Transactional(readOnly = true)
    public Optional<CollectionPlan> findById(UUID id) {
        return planRepository.findById(id);
    }

    // ==================== Create Operations ====================

    @Transactional
    public CollectionPlan create(CollectionPlanCreateRequest request) {
        log.info("Creating collection plan: {}", request.getName());

        CollectionPlan plan = mapper.toEntity(request);

        // Save plan first to get ID
        plan = planRepository.save(plan);

        // Add items if provided
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            if (plan.getItems() == null) {
                plan.setItems(new HashSet<>());
            }
            for (CollectionPlanItemRequest itemReq : request.getItems()) {
                CollectionPlanItem item = mapper.toItemEntity(itemReq, plan);
                plan.getItems().add(item);
            }
            recalculateTotals(plan);
            plan = planRepository.save(plan);
        }

        log.info("Created collection plan with ID: {}", plan.getId());
        return plan;
    }

    // ==================== Update Operations ====================

    @Transactional
    public CollectionPlan update(UUID id, CollectionPlanUpdateRequest request) {
        log.info("Updating collection plan: {}", id);

        CollectionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collection plan not found: " + id));

        if ("CANCELLED".equals(plan.getStatus())) {
            throw new IllegalStateException("Cannot update a cancelled collection plan");
        }

        mapper.updateEntity(plan, request);

        // Update items if provided
        if (request.getItems() != null) {
            // Remove existing items explicitly (no orphanRemoval)
            List<CollectionPlanItem> existingItems = itemRepository.findByCollectionPlanId(id);
            itemRepository.deleteAll(existingItems);
            plan.getItems().clear();

            // Add new items
            for (CollectionPlanItemRequest itemReq : request.getItems()) {
                CollectionPlanItem item = mapper.toItemEntity(itemReq, plan);
                plan.getItems().add(item);
            }
            recalculateTotals(plan);
        }

        plan = planRepository.save(plan);
        log.info("Updated collection plan: {}", id);
        return plan;
    }

    // ==================== Status Operations ====================

    @Transactional
    public CollectionPlan updateStatus(UUID id, String newStatus) {
        log.info("Updating collection plan status: id={}, newStatus={}", id, newStatus);

        CollectionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collection plan not found: " + id));

        validateStatusTransition(plan.getStatus(), newStatus);

        plan.setStatus(newStatus);
        plan = planRepository.save(plan);

        log.info("Updated collection plan {} status to {}", id, newStatus);
        return plan;
    }

    @Transactional
    public void softDelete(UUID id) {
        log.info("Soft deleting collection plan: {}", id);

        CollectionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collection plan not found: " + id));

        if ("COMPLETED".equals(plan.getStatus())) {
            throw new IllegalStateException("Cannot delete a completed collection plan");
        }

        plan.setStatus("CANCELLED");
        planRepository.save(plan);

        log.info("Soft deleted (cancelled) collection plan: {}", id);
    }

    // ==================== Item Operations ====================

    @Transactional
    public CollectionPlan addItem(UUID planId, CollectionPlanItemRequest request) {
        log.info("Adding item to collection plan: {}", planId);

        CollectionPlan plan = planRepository.findByIdWithItems(planId)
                .orElseThrow(() -> new RuntimeException("Collection plan not found: " + planId));

        if ("CANCELLED".equals(plan.getStatus())) {
            throw new IllegalStateException("Cannot add items to a cancelled collection plan");
        }

        CollectionPlanItem item = mapper.toItemEntity(request, plan);
        plan.getItems().add(item);
        recalculateTotals(plan);

        plan = planRepository.save(plan);
        log.info("Added item to collection plan: {}", planId);
        return plan;
    }

    @Transactional
    public CollectionPlan updateItem(UUID planId, UUID itemId, CollectionPlanItemRequest request) {
        log.info("Updating item {} in collection plan: {}", itemId, planId);

        CollectionPlan plan = planRepository.findByIdWithItems(planId)
                .orElseThrow(() -> new RuntimeException("Collection plan not found: " + planId));

        CollectionPlanItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Collection plan item not found: " + itemId));

        if (!item.getCollectionPlan().getId().equals(planId)) {
            throw new IllegalArgumentException("Item does not belong to this collection plan");
        }

        item.setProductName(request.getProductName());
        item.setProductType(request.getProductType());
        item.setBaseDesign(request.getBaseDesign());
        item.setTargetQuantity(request.getTargetQuantity());
        item.setEstimatedUnitCost(request.getEstimatedUnitCost());

        itemRepository.save(item);
        recalculateTotals(plan);
        plan = planRepository.save(plan);

        log.info("Updated item {} in collection plan: {}", itemId, planId);
        return plan;
    }

    @Transactional
    public CollectionPlan removeItem(UUID planId, UUID itemId) {
        log.info("Removing item {} from collection plan: {}", itemId, planId);

        CollectionPlan plan = planRepository.findByIdWithItems(planId)
                .orElseThrow(() -> new RuntimeException("Collection plan not found: " + planId));

        CollectionPlanItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Collection plan item not found: " + itemId));

        if (!item.getCollectionPlan().getId().equals(planId)) {
            throw new IllegalArgumentException("Item does not belong to this collection plan");
        }

        plan.getItems().remove(item);
        itemRepository.delete(item);
        recalculateTotals(plan);
        plan = planRepository.save(plan);

        log.info("Removed item {} from collection plan: {}", itemId, planId);
        return plan;
    }

    // ==================== Stats ====================

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", planRepository.countActive());
        stats.put("DRAFT", planRepository.countByStatus("DRAFT"));
        stats.put("APPROVED", planRepository.countByStatus("APPROVED"));
        stats.put("IN_PROGRESS", planRepository.countByStatus("IN_PROGRESS"));
        stats.put("COMPLETED", planRepository.countByStatus("COMPLETED"));
        return stats;
    }

    @Transactional(readOnly = true)
    public boolean hasProductionPlans(UUID collectionPlanId) {
        return productionPlanRepository.existsByCollectionPlanId(collectionPlanId);
    }

    // ==================== Private Helpers ====================

    private void recalculateTotals(CollectionPlan plan) {
        if (plan.getItems() == null || plan.getItems().isEmpty()) {
            plan.setTotalQuantity(0);
            plan.setTotalCost(BigDecimal.ZERO);
            return;
        }

        int totalQty = 0;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (CollectionPlanItem item : plan.getItems()) {
            if (item.getTargetQuantity() != null) {
                totalQty += item.getTargetQuantity();
            }
            if (item.getTargetQuantity() != null && item.getEstimatedUnitCost() != null) {
                totalCost = totalCost.add(
                        item.getEstimatedUnitCost().multiply(BigDecimal.valueOf(item.getTargetQuantity()))
                );
            }
        }

        plan.setTotalQuantity(totalQty);
        plan.setTotalCost(totalCost);
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus.equals(newStatus)) return;

        Set<String> allowed = switch (currentStatus) {
            case "DRAFT" -> Set.of("APPROVED", "CANCELLED");
            case "APPROVED" -> Set.of("IN_PROGRESS", "CANCELLED");
            case "IN_PROGRESS" -> Set.of("COMPLETED", "CANCELLED");
            case "COMPLETED", "CANCELLED" -> Set.of();
            default -> Set.of();
        };

        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }
}
