package com.mirror.product.repository;

import com.mirror.product.entity.AgeGroupPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for AgeGroupPreference - Age group preferences and attributes
 * Migrated from mirror-mrp-service
 */
@Repository
public interface AgeGroupPreferenceRepository extends JpaRepository<AgeGroupPreference, UUID> {

    List<AgeGroupPreference> findByAgeGroupId(UUID ageGroupId);

    List<AgeGroupPreference> findByAttributeType(String attributeType);
}
