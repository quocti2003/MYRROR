package com.mirror.product.repository;

import com.mirror.product.entity.InventoryPosition;
import com.mirror.product.entity.InventoryPosition.PositionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryPositionRepository extends JpaRepository<InventoryPosition, String> {

    Optional<InventoryPosition> findByProductCodeAndIsDeletedFalse(String productCode);

    Optional<InventoryPosition> findByIdAndIsDeletedFalse(String id);

    List<InventoryPosition> findByPositionStatusAndIsDeletedFalse(PositionStatus status);

    Page<InventoryPosition> findByPositionStatusAndIsDeletedFalse(PositionStatus status, Pageable pageable);

    List<InventoryPosition> findByWarehouseIdAndIsDeletedFalse(String warehouseId);

    List<InventoryPosition> findByRackIdAndIsDeletedFalse(String rackId);

    List<InventoryPosition> findBySlotIdAndIsDeletedFalse(String slotId);

    // Find items pending placement (need location assignment)
    @Query("SELECT p FROM InventoryPosition p WHERE p.positionStatus = 'PENDING_PLACEMENT' " +
           "AND p.isDeleted = false AND p.isActive = true " +
           "ORDER BY p.createdAt DESC")
    List<InventoryPosition> findPendingPlacements();

    @Query("SELECT p FROM InventoryPosition p WHERE p.positionStatus = 'PENDING_PLACEMENT' " +
           "AND p.isDeleted = false AND p.isActive = true")
    Page<InventoryPosition> findPendingPlacements(Pageable pageable);

    // Count pending placements for dashboard alerts
    @Query("SELECT COUNT(p) FROM InventoryPosition p WHERE p.positionStatus = 'PENDING_PLACEMENT' " +
           "AND p.isDeleted = false AND p.isActive = true")
    long countPendingPlacements();

    // Find positions by warehouse with stats
    @Query("SELECT p FROM InventoryPosition p WHERE p.warehouse.id = :warehouseId " +
           "AND p.isDeleted = false ORDER BY p.productCode")
    List<InventoryPosition> findByWarehouseIdWithDetails(@Param("warehouseId") String warehouseId);

    // Find positions that have quantity but no complete location
    @Query("SELECT p FROM InventoryPosition p WHERE p.quantity > 0 " +
           "AND (p.warehouse IS NULL OR p.slot IS NULL) " +
           "AND p.isDeleted = false")
    List<InventoryPosition> findIncompletePositions();

    // Find positions by product codes (batch lookup)
    @Query("SELECT p FROM InventoryPosition p WHERE p.productCode IN :productCodes AND p.isDeleted = false")
    List<InventoryPosition> findByProductCodes(@Param("productCodes") List<String> productCodes);

    // Stats queries
    @Query("SELECT COUNT(DISTINCT p.productCode) FROM InventoryPosition p " +
           "WHERE p.warehouse.id = :warehouseId AND p.isDeleted = false")
    long countProductsInWarehouse(@Param("warehouseId") String warehouseId);

    @Query("SELECT SUM(p.quantity) FROM InventoryPosition p " +
           "WHERE p.warehouse.id = :warehouseId AND p.isDeleted = false")
    Long sumQuantityInWarehouse(@Param("warehouseId") String warehouseId);

    Page<InventoryPosition> findByIsDeletedFalse(Pageable pageable);

    boolean existsByProductCodeAndIsDeletedFalse(String productCode);
}
