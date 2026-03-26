package com.mirror.product.repository;

import com.mirror.product.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for PurchaseOrderItem - Line items in purchase orders
 * Migrated from mirror-mrp-service PoItemRepository
 */
@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, UUID> {

    List<PurchaseOrderItem> findByPurchaseOrderId(UUID purchaseOrderId);

    List<PurchaseOrderItem> findByMaterialId(UUID materialId);
}
