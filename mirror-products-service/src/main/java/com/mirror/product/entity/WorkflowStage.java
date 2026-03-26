package com.mirror.product.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.PartnerCapabilityType;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * WorkflowStage Entity
 *
 * Represents a single stage within a workflow template.
 * Stages are ordered by stageOrder and can require specific partner capabilities.
 *
 * Stage dependency logic:
 * - First stage (stageOrder = 1) starts as READY
 * - Subsequent stages start as BLOCKED
 * - A stage becomes READY when the previous stage completes
 * - isFinalStage marks the delivery stage to MIRROR
 */
@Entity
@Table(name = "workflow_stages", indexes = {
    @Index(name = "idx_wfs_template", columnList = "template_id"),
    @Index(name = "idx_wfs_stage_order", columnList = "stage_order"),
    @Index(name = "idx_wfs_capability", columnList = "required_capability")
}, uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_template_stage_order",
        columnNames = {"template_id", "stage_order"}
    ),
    @UniqueConstraint(
        name = "uk_template_stage_name",
        columnNames = {"template_id", "name"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WorkflowStage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @JsonBackReference("template-stages")
    private WorkflowTemplate template;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder; // Sequence in workflow (1, 2, 3, ...)

    @Enumerated(EnumType.STRING)
    @Column(name = "required_capability")
    private PartnerCapabilityType requiredCapability; // Optional capability requirement

    @Column(name = "estimated_duration_days")
    private Integer estimatedDurationDays;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions; // Detailed instructions for this stage

    @Column(name = "is_final_stage")
    @Builder.Default
    private Boolean isFinalStage = false; // Marks delivery to MIRROR

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.WFS));
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
}
