package com.mirror.product.entity;

import jakarta.persistence.*;
import com.mirror.product.util.SequenceIdGenerator;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.config.ApplicationContextProvider;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "component_optionals")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentOptional extends BaseEntity {
    
    @Column(name = "component_optional_name", nullable = false)
    private String componentOptionalName;
    
    @Column(length = 500)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", referencedColumnName = "id", nullable = false)
    private Component component;
    
    @OneToMany(mappedBy = "componentOptional", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemVariantConfig> variantConfigs = new ArrayList<>();
    
    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.OPT));
        }
    }
}