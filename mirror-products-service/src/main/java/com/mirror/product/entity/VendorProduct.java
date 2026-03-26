package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.mirror.product.util.SequenceIdGenerator;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.config.ApplicationContextProvider;

@Entity
@Table(name = "vendor_products")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VendorProduct extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    @ToString.Exclude
    private Vendor vendor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    @ToString.Exclude
    private MirrorProduct product;
    
    @Column(name = "commission_term", columnDefinition = "TEXT")
    private String commissionTerm;
    
    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.VPR));
        }
    }
}