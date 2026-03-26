package com.mirror.product.entity;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.ProductionPlanStatus;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * ProductionPlan Entity
 *
 * Represents a production plan that groups production orders together.
 * A production plan can be linked to a CollectionPlan (for seasonal collections)
 * and uses a WorkflowTemplate to define the production stages.
 *
 * Status workflow:
 * DRAFT -> PLANNING -> APPROVED -> IN_PRODUCTION -> COMPLETED
 *                                               \-> CANCELLED
 */
@Entity
@Table(name = "production_plans", indexes = {
    @Index(name = "idx_ppl_name", columnList = "name"),
    @Index(name = "idx_ppl_collection_plan", columnList = "collection_plan_id"),
    @Index(name = "idx_ppl_workflow_template", columnList = "workflow_template_id"),
    @Index(name = "idx_ppl_status", columnList = "status"),
    @Index(name = "idx_ppl_target_start", columnList = "target_start_date"),
    @Index(name = "idx_ppl_target_end", columnList = "target_end_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductionPlan extends BaseEntity {

    /**
     * Version field for optimistic locking.
     * Prevents race conditions during concurrent approval/rejection operations.
     * JPA automatically increments this on each update.
     */
    @Version
    @Column(name = "version")
    @Builder.Default
    private Long version = 0L;

    @Column(name = "name", nullable = false)
    private String name;

    // Optional link to CollectionPlan for seasonal collections
    @Column(name = "collection_plan_id")
    private UUID collectionPlanId;

    // Required link to WorkflowTemplate
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_template_id", nullable = false)
    private WorkflowTemplate workflowTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ProductionPlanStatus status = ProductionPlanStatus.DRAFT;

    // Target dates for planning
    @Column(name = "target_start_date")
    private LocalDate targetStartDate;

    @Column(name = "target_end_date")
    private LocalDate targetEndDate;

    // Actual dates for tracking
    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Audit fields
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // Note: productionOrders (OneToMany) will be added in Sprint 4
    // when ProductionOrder entity is created

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PPL));
        }
        if (status == null) {
            status = ProductionPlanStatus.DRAFT;
        }
    }

    /**
     * Check if the plan can be modified
     */
    public boolean isEditable() {
        return status == ProductionPlanStatus.DRAFT || status == ProductionPlanStatus.PLANNING;
    }

    /**
     * Check if the plan can be cancelled
     */
    public boolean isCancellable() {
        return status != ProductionPlanStatus.COMPLETED && status != ProductionPlanStatus.CANCELLED;
    }

    /**
     * Check if the plan is in an active state
     */
    public boolean isActive() {
        return status == ProductionPlanStatus.APPROVED || status == ProductionPlanStatus.IN_PRODUCTION;
    }
}
