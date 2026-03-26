package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.mirror.product.util.SequenceIdGenerator;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.config.ApplicationContextProvider;

@Entity
@Table(name = "collection_products")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionProduct extends BaseEntity {

    @Column(name = "collection_id", nullable = false)
    private String collectionId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_hero_product")
    private Boolean isHeroProduct = false;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    private MirrorProduct product;

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.CPR));
        }
    }

    @Override
    public String toString() {
        return "CollectionProduct{" +
                "id='" + getId() + '\'' +
                ", collectionId='" + collectionId + '\'' +
                ", productId='" + productId + '\'' +
                ", sortOrder=" + sortOrder +
                ", isHeroProduct=" + isHeroProduct +
                '}';
    }
}