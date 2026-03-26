package com.mirror.product.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

/**
 * Age Group - Customer demographic segments for collection planning and analytics
 * Migrated from mirror-mrp-service
 */
@Entity
@Table(name = "age_groups")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgeGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @OneToMany(mappedBy = "ageGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("ageGroup-preferences")
    @ToString.Exclude
    private Set<AgeGroupPreference> ageGroupPreferences;

    @OneToMany(mappedBy = "ageGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private Set<PurchaseOrderAgeGroupAllocation> purchaseOrderAgeGroupAllocations;

    @OneToMany(mappedBy = "ageGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private Set<PurchaseOrderItemVariant> orderItemVariants;
}
