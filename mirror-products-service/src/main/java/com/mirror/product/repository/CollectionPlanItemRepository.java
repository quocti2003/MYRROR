package com.mirror.product.repository;

import com.mirror.product.entity.CollectionPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for CollectionPlanItem - Individual items in a collection plan
 * Migrated from mirror-mrp-service (Phase 4)
 */
@Repository
public interface CollectionPlanItemRepository extends JpaRepository<CollectionPlanItem, UUID> {

    List<CollectionPlanItem> findByCollectionPlanId(UUID collectionPlanId);

    List<CollectionPlanItem> findByProductType(String productType);
}
