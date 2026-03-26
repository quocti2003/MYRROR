package com.mirror.product.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.WorkflowTemplateStatus;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * WorkflowTemplate Entity
 *
 * Defines a reusable production workflow consisting of ordered stages.
 * Templates can be category-specific (RING, NECKLACE, EARRING) and one
 * template per category can be marked as the default.
 *
 * Workflow stages are ordered and define the sequence of production steps,
 * each potentially requiring specific partner capabilities.
 */
@Entity
@Table(name = "workflow_templates", indexes = {
    @Index(name = "idx_wft_name", columnList = "name"),
    @Index(name = "idx_wft_category", columnList = "category"),
    @Index(name = "idx_wft_status", columnList = "status"),
    @Index(name = "idx_wft_is_default", columnList = "is_default")
}, uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_workflow_template_name",
        columnNames = {"name"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WorkflowTemplate extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category")
    private String category; // e.g., "RING", "NECKLACE", "EARRING"

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false; // Default template for the category

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private WorkflowTemplateStatus status = WorkflowTemplateStatus.DRAFT;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stageOrder ASC")
    @JsonManagedReference("template-stages")
    @Builder.Default
    private List<WorkflowStage> stages = new ArrayList<>();

    // Audit fields
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.WFT));
        }
        if (status == null) {
            status = WorkflowTemplateStatus.DRAFT;
        }
        if (isDefault == null) {
            isDefault = false;
        }
    }

    // Helper methods for managing stages
    public void addStage(WorkflowStage stage) {
        if (stages == null) {
            stages = new ArrayList<>();
        }
        stages.add(stage);
        stage.setTemplate(this);
    }

    public void removeStage(WorkflowStage stage) {
        if (stages != null) {
            stages.remove(stage);
            stage.setTemplate(null);
        }
    }

    public void clearStages() {
        if (stages != null) {
            stages.forEach(stage -> stage.setTemplate(null));
            stages.clear();
        }
    }

    /**
     * Get the total estimated duration in days for all stages
     */
    public Integer getTotalEstimatedDurationDays() {
        if (stages == null || stages.isEmpty()) {
            return 0;
        }
        return stages.stream()
                .filter(s -> s.getEstimatedDurationDays() != null)
                .mapToInt(WorkflowStage::getEstimatedDurationDays)
                .sum();
    }

    /**
     * Get the number of stages in this template
     */
    public int getStageCount() {
        return stages != null ? stages.size() : 0;
    }
}
