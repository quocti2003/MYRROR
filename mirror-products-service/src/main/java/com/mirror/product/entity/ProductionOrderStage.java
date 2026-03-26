package com.mirror.product.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.PartnerCapabilityType;
import com.mirror.product.enums.ProductionOrderStageStatus;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ProductionOrderStage Entity
 *
 * Represents a single stage within a production order.
 * Stages are created from the WorkflowTemplate when an order is generated.
 * Each stage tracks its own progress and can be assigned to a specific vendor.
 *
 * Stage dependency logic:
 * - First stage (stageOrder = 1) starts as READY
 * - Subsequent stages start as BLOCKED
 * - A stage becomes READY when the previous stage completes
 * - isFinalStage marks the delivery stage to MIRROR
 */
@Entity
@Table(name = "production_order_stages", indexes = {
    @Index(name = "idx_pos_production_order", columnList = "production_order_id"),
    @Index(name = "idx_pos_workflow_stage", columnList = "workflow_stage_id"),
    @Index(name = "idx_pos_stage_order", columnList = "stage_order"),
    @Index(name = "idx_pos_status", columnList = "status"),
    @Index(name = "idx_pos_assigned_vendor", columnList = "assigned_vendor_id"),
    @Index(name = "idx_pos_capability", columnList = "required_capability")
}, uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_order_stage_order",
        columnNames = {"production_order_id", "stage_order"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductionOrderStage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    @JsonBackReference("order-stages")
    private ProductionOrder productionOrder;

    // Reference to the template stage this was created from
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_stage_id", nullable = false)
    private WorkflowStage workflowStage;

    // Copied from template for denormalization/convenience
    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Column(name = "stage_name", nullable = false)
    private String stageName;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_capability")
    private PartnerCapabilityType requiredCapability;

    // Vendor assignment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_vendor_id")
    private Vendor assignedVendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ProductionOrderStageStatus status = ProductionOrderStageStatus.BLOCKED;

    // Planned dates
    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    // Actual dates
    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    // Cost tracking
    @Column(name = "estimated_cost", precision = 15, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 15, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "is_final_stage")
    @Builder.Default
    private Boolean isFinalStage = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Completion tracking
    @Column(name = "completed_by")
    private String completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.POS));
        }
        if (status == null) {
            status = ProductionOrderStageStatus.BLOCKED;
        }
        if (isFinalStage == null) {
            isFinalStage = false;
        }
    }

    /**
     * Check if this is the first stage in the workflow
     */
    public boolean isFirstStage() {
        return stageOrder != null && stageOrder == 1;
    }

    /**
     * Check if stage can be started
     */
    public boolean canStart() {
        return status == ProductionOrderStageStatus.READY;
    }

    /**
     * Check if stage can be completed
     */
    public boolean canComplete() {
        return status == ProductionOrderStageStatus.IN_PROGRESS;
    }

    /**
     * Check if stage can be skipped
     */
    public boolean canSkip() {
        return status == ProductionOrderStageStatus.BLOCKED || status == ProductionOrderStageStatus.READY;
    }

    /**
     * Check if stage is in a terminal state
     */
    public boolean isTerminal() {
        return status == ProductionOrderStageStatus.COMPLETED || status == ProductionOrderStageStatus.SKIPPED;
    }

    /**
     * Check if vendor is assigned
     */
    public boolean hasVendorAssigned() {
        return assignedVendor != null;
    }

    /**
     * Check if a vendor can be assigned to this stage.
     * Vendors can be assigned to stages that are not in a terminal state (COMPLETED or SKIPPED).
     * This moves the assignability logic from frontend to backend.
     */
    public boolean canAssign() {
        return status != ProductionOrderStageStatus.COMPLETED && status != ProductionOrderStageStatus.SKIPPED;
    }
}
