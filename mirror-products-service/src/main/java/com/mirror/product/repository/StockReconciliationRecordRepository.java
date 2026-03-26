package com.mirror.product.repository;

import com.mirror.product.entity.StockReconciliationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReconciliationRecordRepository extends JpaRepository<StockReconciliationRecord, String> {

    Optional<StockReconciliationRecord> findByIdAndIsDeletedFalse(String id);

    Optional<StockReconciliationRecord> findByDocumentNumberAndIsDeletedFalse(String documentNumber);

    Page<StockReconciliationRecord> findByIsDeletedFalseOrderByReconciliationDateDesc(Pageable pageable);

    Page<StockReconciliationRecord> findByWarehouseIdAndIsDeletedFalseOrderByReconciliationDateDesc(
            String warehouseId, Pageable pageable);

    @Query("SELECT r FROM StockReconciliationRecord r WHERE r.isDeleted = false " +
           "AND (:warehouseId IS NULL OR r.warehouseId = :warehouseId) " +
           "AND (:createdBy IS NULL OR r.createdBy = :createdBy) " +
           "AND (:startDate IS NULL OR r.reconciliationDate >= :startDate) " +
           "AND (:endDate IS NULL OR r.reconciliationDate <= :endDate) " +
           "ORDER BY r.reconciliationDate DESC")
    Page<StockReconciliationRecord> findWithFilters(
            @Param("warehouseId") String warehouseId,
            @Param("createdBy") String createdBy,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    List<StockReconciliationRecord> findByWarehouseIdAndIsDeletedFalse(String warehouseId);

    @Query("SELECT COUNT(r) FROM StockReconciliationRecord r WHERE r.isDeleted = false " +
           "AND r.warehouseId = :warehouseId")
    long countByWarehouseId(@Param("warehouseId") String warehouseId);

    boolean existsByDocumentNumberAndIsDeletedFalse(String documentNumber);
}
