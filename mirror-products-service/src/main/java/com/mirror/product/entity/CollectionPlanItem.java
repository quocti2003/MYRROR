package com.mirror.product.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Collection Plan Item - Individual items in a collection plan
 * Migrated from mirror-mrp-service (Phase 4)
 */
@Entity
@Table(name = "collection_plan_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_plan_id", nullable = false)
    @JsonBackReference("collectionPlan-items")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CollectionPlan collectionPlan;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_type", nullable = false)
    private String productType;

    @Column(name = "base_design")
    private String baseDesign;

    @Column(name = "target_quantity", nullable = false)
    private Integer targetQuantity;

    @Column(name = "estimated_unit_cost", precision = 10, scale = 2)
    private BigDecimal estimatedUnitCost;

    @Column(name = "jewelry_spec_id")
    private String jewelrySpecId;
}
