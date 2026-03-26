package com.mirror.product.repository;

import com.mirror.product.entity.AgeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AgeGroup - Customer demographic segments
 * Migrated from mirror-mrp-service
 */
@Repository
public interface AgeGroupRepository extends JpaRepository<AgeGroup, UUID> {

    Optional<AgeGroup> findByName(String name);
}
