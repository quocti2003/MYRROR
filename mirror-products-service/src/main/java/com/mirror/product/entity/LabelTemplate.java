package com.mirror.product.entity;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.LabelType;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * LabelTemplate Entity
 *
 * Stores ZPL templates for printing product labels on Zebra printers.
 * Templates contain placeholders ({{VARIABLE}}) that are replaced with
 * actual product data when rendering.
 */
@Entity
@Table(name = "label_templates", indexes = {
    @Index(name = "idx_lbt_name", columnList = "name"),
    @Index(name = "idx_lbt_label_type", columnList = "label_type"),
    @Index(name = "idx_lbt_is_default", columnList = "is_default"),
    @Index(name = "idx_lbt_is_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_label_template_name",
        columnNames = {"name"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LabelTemplate extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "zpl_content", columnDefinition = "TEXT", nullable = false)
    private String zplContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "label_type", nullable = false, length = 50)
    private LabelType labelType;

    @Column(name = "width_mm")
    private Integer widthMm;

    @Column(name = "height_mm")
    private Integer heightMm;

    @Column(name = "dpi", nullable = false)
    @Builder.Default
    private Integer dpi = 203;

    /**
     * JSON array of variable names used in the template
     * e.g., ["SKU", "PRICE", "BARCODE", "PRODUCT_NAME"]
     */
    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables;

    @Column(name = "preview_image_url", length = 500)
    private String previewImageUrl;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.LBT));
        }
        if (dpi == null) {
            dpi = 203;
        }
        if (isDefault == null) {
            isDefault = false;
        }
    }
}
