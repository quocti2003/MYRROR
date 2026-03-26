package com.mirror.product.repository;

import com.mirror.product.entity.CollectionAgeGroupAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for CollectionAgeGroupAllocation - Allocate collection quantities by demographics
 * Migrated from mirror-mrp-service (Phase 4)
 */
@Repository
public interface CollectionAgeGroupAllocationRepository extends JpaRepository<CollectionAgeGroupAllocation, UUID> {

    List<CollectionAgeGroupAllocation> findByCollectionPlanId(UUID collectionPlanId);

    List<CollectionAgeGroupAllocation> findByAgeGroupId(UUID ageGroupId);
}
