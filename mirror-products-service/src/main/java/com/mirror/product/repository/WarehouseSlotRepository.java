package com.mirror.product.repository;

import com.mirror.product.entity.WarehouseSlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseSlotRepository extends JpaRepository<WarehouseSlot, String> {

    List<WarehouseSlot> findByRackIdAndIsDeletedFalse(String rackId);

    Optional<WarehouseSlot> findByIdAndIsDeletedFalse(String id);

    Optional<WarehouseSlot> findByRackIdAndSlotCodeAndIsDeletedFalse(String rackId, String slotCode);

    boolean existsByRackIdAndSlotCodeAndIsDeletedFalse(String rackId, String slotCode);

    Optional<WarehouseSlot> findByCurrentProductCodeAndIsDeletedFalse(String productCode);

    @Query("SELECT s FROM WarehouseSlot s WHERE s.rack.id = :rackId " +
           "AND s.isDeleted = false AND s.isActive = true AND s.isOccupied = false " +
           "ORDER BY s.rowInRack, s.columnInRack")
    List<WarehouseSlot> findAvailableSlotsByRackId(@Param("rackId") String rackId);

    @Query("SELECT s FROM WarehouseSlot s WHERE s.rack.id = :rackId " +
           "AND s.isDeleted = false AND s.isOccupied = true")
    List<WarehouseSlot> findOccupiedSlotsByRackId(@Param("rackId") String rackId);

    @Query("SELECT s FROM WarehouseSlot s WHERE s.rack.warehouse.id = :warehouseId " +
           "AND s.isDeleted = false AND s.isActive = true AND s.isOccupied = false")
    List<WarehouseSlot> findAvailableSlotsByWarehouseId(@Param("warehouseId") String warehouseId);

    @Query("SELECT s FROM WarehouseSlot s WHERE s.rack.id = :rackId " +
           "AND s.isDeleted = false AND s.slotType = :slotType")
    List<WarehouseSlot> findByRackIdAndSlotType(
            @Param("rackId") String rackId,
            @Param("slotType") WarehouseSlot.SlotType slotType);

    @Query("SELECT COUNT(s) FROM WarehouseSlot s WHERE s.rack.id = :rackId AND s.isDeleted = false")
    long countByRackId(@Param("rackId") String rackId);

    @Query("SELECT COUNT(s) FROM WarehouseSlot s WHERE s.rack.id = :rackId " +
           "AND s.isDeleted = false AND s.isOccupied = true")
    long countOccupiedByRackId(@Param("rackId") String rackId);

    @Query("SELECT COUNT(s) FROM WarehouseSlot s WHERE s.rack.warehouse.id = :warehouseId " +
           "AND s.isDeleted = false AND s.isOccupied = false AND s.isActive = true")
    long countAvailableByWarehouseId(@Param("warehouseId") String warehouseId);

    Page<WarehouseSlot> findByIsDeletedFalse(Pageable pageable);
}
