package com.mirror.product.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Age Group Preference - Preferences and attributes for different age groups
 * (e.g., preferred metals, stone shapes, price ranges)
 * Migrated from mirror-mrp-service
 */
@Entity
@Table(name = "age_group_preferences")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgeGroupPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "age_group_id", nullable = false)
    @JsonBackReference("ageGroup-preferences")
    @ToString.Exclude
    private AgeGroup ageGroup;

    @Column(name = "attribute_type", nullable = false)
    private String attributeType;

    @ElementCollection
    @CollectionTable(name = "age_group_preference_values", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "preferred_value")
    private List<String> preferredValues;

    @Column(nullable = false)
    private Integer weight;
}
