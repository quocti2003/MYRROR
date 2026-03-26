package com.mirror.product.repository;

import com.mirror.product.entity.InventoryPositionHistory;
import com.mirror.product.entity.InventoryPositionHistory.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryPositionHistoryRepository extends JpaRepository<InventoryPositionHistory, String> {

    List<InventoryPositionHistory> findByPositionIdOrderByPerformedAtDesc(String positionId);

    Page<InventoryPositionHistory> findByPositionIdOrderByPerformedAtDesc(String positionId, Pageable pageable);

    List<InventoryPositionHistory> findByProductCodeOrderByPerformedAtDesc(String productCode);

    Page<InventoryPositionHistory> findByProductCodeOrderByPerformedAtDesc(String productCode, Pageable pageable);

    List<InventoryPositionHistory> findByActionTypeOrderByPerformedAtDesc(ActionType actionType);

    List<InventoryPositionHistory> findByPerformedByOrderByPerformedAtDesc(String performedBy);

    // Find history within date range
    @Query("SELECT h FROM InventoryPositionHistory h WHERE h.performedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY h.performedAt DESC")
    List<InventoryPositionHistory> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT h FROM InventoryPositionHistory h WHERE h.performedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY h.performedAt DESC")
    Page<InventoryPositionHistory> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Find history by warehouse
    @Query("SELECT h FROM InventoryPositionHistory h WHERE " +
           "(h.fromWarehouseId = :warehouseId OR h.toWarehouseId = :warehouseId) " +
           "ORDER BY h.performedAt DESC")
    List<InventoryPositionHistory> findByWarehouseId(@Param("warehouseId") String warehouseId);

    @Query("SELECT h FROM InventoryPositionHistory h WHERE " +
           "(h.fromWarehouseId = :warehouseId OR h.toWarehouseId = :warehouseId) " +
           "ORDER BY h.performedAt DESC")
    Page<InventoryPositionHistory> findByWarehouseId(@Param("warehouseId") String warehouseId, Pageable pageable);

    // Find movements (MOVED action type)
    @Query("SELECT h FROM InventoryPositionHistory h WHERE h.actionType = 'MOVED' " +
           "ORDER BY h.performedAt DESC")
    Page<InventoryPositionHistory> findAllMovements(Pageable pageable);

    // Find recent activity
    @Query("SELECT h FROM InventoryPositionHistory h ORDER BY h.performedAt DESC")
    Page<InventoryPositionHistory> findRecentActivity(Pageable pageable);

    // Count by action type within date range
    @Query("SELECT COUNT(h) FROM InventoryPositionHistory h " +
           "WHERE h.actionType = :actionType AND h.performedAt BETWEEN :startDate AND :endDate")
    long countByActionTypeAndDateRange(
            @Param("actionType") ActionType actionType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Find by reference
    @Query("SELECT h FROM InventoryPositionHistory h WHERE h.referenceType = :refType AND h.referenceId = :refId " +
           "ORDER BY h.performedAt DESC")
    List<InventoryPositionHistory> findByReference(
            @Param("refType") String refType,
            @Param("refId") String refId);
}
