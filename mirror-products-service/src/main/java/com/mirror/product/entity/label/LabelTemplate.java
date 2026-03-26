package com.mirror.product.entity.label;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.LabelTemplateStatus;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a label design template.
 * Stores the canvas design data (Fabric.js JSON) and label specifications.
 */
@Entity(name = "RFIDLabelTemplate")
@Table(name = "label_templates", indexes = {
    @Index(name = "idx_label_templates_status", columnList = "status"),
    @Index(name = "idx_label_templates_is_default", columnList = "is_default"),
    @Index(name = "idx_label_templates_name", columnList = "name")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LabelTemplate extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "label_width", nullable = false, precision = 10, scale = 2)
    private BigDecimal labelWidth;

    @Column(name = "label_height", nullable = false, precision = 10, scale = 2)
    private BigDecimal labelHeight;

    @Column(name = "dpi", nullable = false)
    @Builder.Default
    private Integer dpi = 300;

    @Column(name = "canvas_json", nullable = false, columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String canvasJson;

    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private LabelTemplateStatus status = LabelTemplateStatus.ACTIVE;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PrintJob> printJobs = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.LBT));
        }
    }

    /**
     * Calculate label width in dots based on DPI
     */
    public int getWidthInDots() {
        return (int) Math.round(labelWidth.doubleValue() / 25.4 * dpi);
    }

    /**
     * Calculate label height in dots based on DPI
     */
    public int getHeightInDots() {
        return (int) Math.round(labelHeight.doubleValue() / 25.4 * dpi);
    }
}
