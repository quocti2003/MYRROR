package com.mirror.product.repository;

import com.mirror.product.entity.WarehouseRack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRackRepository extends JpaRepository<WarehouseRack, String> {

    List<WarehouseRack> findByWarehouseIdAndIsDeletedFalse(String warehouseId);

    Optional<WarehouseRack> findByIdAndIsDeletedFalse(String id);

    Optional<WarehouseRack> findByWarehouseIdAndRackCodeAndIsDeletedFalse(String warehouseId, String rackCode);

    boolean existsByWarehouseIdAndRackCodeAndIsDeletedFalse(String warehouseId, String rackCode);

    @Query("SELECT r FROM WarehouseRack r WHERE r.warehouse.id = :warehouseId " +
           "AND r.isDeleted = false AND r.isActive = true " +
           "ORDER BY r.floorLevel, r.rowPosition, r.columnPosition")
    List<WarehouseRack> findActiveRacksByWarehouseId(@Param("warehouseId") String warehouseId);

    @Query("SELECT r FROM WarehouseRack r WHERE r.warehouse.id = :warehouseId " +
           "AND r.isDeleted = false AND r.rackType = :rackType")
    List<WarehouseRack> findByWarehouseIdAndRackType(
            @Param("warehouseId") String warehouseId,
            @Param("rackType") WarehouseRack.RackType rackType);

    @Query("SELECT COUNT(r) FROM WarehouseRack r WHERE r.warehouse.id = :warehouseId AND r.isDeleted = false")
    long countByWarehouseId(@Param("warehouseId") String warehouseId);

    Page<WarehouseRack> findByIsDeletedFalse(Pageable pageable);
}
