package com.mirror.product.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

import java.util.UUID;

/**
 * Purchase Order Age Group Allocation - Allocates purchase order quantities by age group demographics
 * Migrated from mirror-mrp-service OrderAgeGroupAllocation entity
 */
@Entity
@Table(name = "purchase_order_age_group_allocations")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderAgeGroupAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "age_group_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private AgeGroup ageGroup;

    @Column(name = "allocation_percentage", nullable = false)
    private BigDecimal allocationPercentage;

    @Column
    private Integer quantity;
}
