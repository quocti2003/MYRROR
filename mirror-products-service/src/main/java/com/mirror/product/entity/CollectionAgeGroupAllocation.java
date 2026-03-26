package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Collection Age Group Allocation - Allocate collection quantities by demographics
 * Migrated from mirror-mrp-service (Phase 4)
 */
@Entity
@Table(name = "collection_age_group_allocations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionAgeGroupAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_plan_id", nullable = false)
    private CollectionPlan collectionPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "age_group_id", nullable = false)
    private AgeGroup ageGroup;

    @Column(name = "allocation_percentage", nullable = false)
    private Integer allocationPercentage;

    @Column(name = "target_quantity", nullable = false)
    private Integer targetQuantity;
}
