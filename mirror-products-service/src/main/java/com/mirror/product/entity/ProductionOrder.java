package com.mirror.product.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.ProductionOrderStatus;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ProductionOrder Entity
 *
 * Represents a single production order for creating a jewelry piece.
 * Each order tracks progress through multiple stages defined by the
 * workflow template associated with its production plan.
 *
 * Status workflow:
 * DRAFT -> READY -> IN_PROGRESS -> COMPLETED
 *                              \-> CANCELLED
 */
@Entity
@Table(name = "production_orders", indexes = {
    @Index(name = "idx_por_order_number", columnList = "order_number"),
    @Index(name = "idx_por_production_plan", columnList = "production_plan_id"),
    @Index(name = "idx_por_collection_plan_item", columnList = "collection_plan_item_id"),
    @Index(name = "idx_por_jtrc", columnList = "jtrc_id"),
    @Index(name = "idx_por_status", columnList = "status"),
    @Index(name = "idx_por_current_holder", columnList = "current_holder_id"),
    @Index(name = "idx_por_estimated_completion", columnList = "estimated_completion_date")
}, uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_production_order_number",
        columnNames = {"order_number"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductionOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_plan_id", nullable = false)
    private ProductionPlan productionPlan;

    // Optional link to CollectionPlanItem
    @Column(name = "collection_plan_item_id")
    private UUID collectionPlanItemId;

    // Optional link to JTRC (Jewelry Technical Report Card)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jtrc_id")
    private JewelryTechnicalReport jtrc;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber; // Format: "PO-YYYY-MM-####"

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ProductionOrderStatus status = ProductionOrderStatus.DRAFT;

    // Current position in workflow
    @Column(name = "current_stage_order")
    private Integer currentStageOrder; // Which stage is currently active

    @Column(name = "current_holder_id")
    private String currentHolderId; // Vendor ID currently holding the component

    // Date tracking
    @Column(name = "estimated_completion_date")
    private LocalDate estimatedCompletionDate;

    @Column(name = "actual_completion_date")
    private LocalDate actualCompletionDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Flag to indicate if this order needs specification review
     * Used for P3-10: JTRC to Production Order link enhancement
     * True when the linked JTRC specs have been modified after order creation
     */
    @Column(name = "needs_spec_review")
    @Builder.Default
    private Boolean needsSpecReview = false;

    // Stages for this order
    @OneToMany(mappedBy = "productionOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stageOrder ASC")
    @JsonManagedReference("order-stages")
    @Builder.Default
    private List<ProductionOrderStage> stages = new ArrayList<>();

    // Audit fields
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.POR));
        }
        if (status == null) {
            status = ProductionOrderStatus.DRAFT;
        }
        if (quantity == null) {
            quantity = 1;
        }
    }

    // Helper methods for managing stages
    public void addStage(ProductionOrderStage stage) {
        if (stages == null) {
            stages = new ArrayList<>();
        }
        stages.add(stage);
        stage.setProductionOrder(this);
    }

    public void removeStage(ProductionOrderStage stage) {
        if (stages != null) {
            stages.remove(stage);
            stage.setProductionOrder(null);
        }
    }

    public void clearStages() {
        if (stages != null) {
            stages.forEach(stage -> stage.setProductionOrder(null));
            stages.clear();
        }
    }

    /**
     * Get the current active stage (if any)
     */
    public ProductionOrderStage getCurrentStage() {
        if (stages == null || stages.isEmpty() || currentStageOrder == null) {
            return null;
        }
        return stages.stream()
                .filter(s -> s.getStageOrder().equals(currentStageOrder))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the next stage after current (if any)
     */
    public ProductionOrderStage getNextStage() {
        if (stages == null || stages.isEmpty() || currentStageOrder == null) {
            return null;
        }
        return stages.stream()
                .filter(s -> s.getStageOrder() > currentStageOrder)
                .min((a, b) -> a.getStageOrder().compareTo(b.getStageOrder()))
                .orElse(null);
    }

    /**
     * Check if the order can be modified
     */
    public boolean isEditable() {
        return status == ProductionOrderStatus.DRAFT || status == ProductionOrderStatus.READY;
    }

    /**
     * Check if the order can be cancelled
     */
    public boolean isCancellable() {
        return status != ProductionOrderStatus.COMPLETED && status != ProductionOrderStatus.CANCELLED;
    }

    /**
     * Check if the order is in a terminal state
     */
    public boolean isTerminal() {
        return status == ProductionOrderStatus.COMPLETED || status == ProductionOrderStatus.CANCELLED;
    }

    /**
     * Get the number of completed stages
     */
    public int getCompletedStageCount() {
        if (stages == null || stages.isEmpty()) {
            return 0;
        }
        return (int) stages.stream()
                .filter(s -> s.getStatus() == com.mirror.product.enums.ProductionOrderStageStatus.COMPLETED)
                .count();
    }

    /**
     * Get total stage count
     */
    public int getTotalStageCount() {
        return stages != null ? stages.size() : 0;
    }

    /**
     * Calculate progress percentage
     */
    public int getProgressPercentage() {
        int total = getTotalStageCount();
        if (total == 0) {
            return 0;
        }
        return (getCompletedStageCount() * 100) / total;
    }
}
