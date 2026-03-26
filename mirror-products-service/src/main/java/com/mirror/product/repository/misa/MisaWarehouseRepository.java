package com.mirror.product.repository.misa;

import com.mirror.product.entity.misa.MisaWarehouse;
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
public interface MisaWarehouseRepository extends JpaRepository<MisaWarehouse, Long> {

    Optional<MisaWarehouse> findByStockId(String stockId);

    Optional<MisaWarehouse> findByStockCode(String stockCode);

    List<MisaWarehouse> findByIsInactiveFalse();

    Page<MisaWarehouse> findByIsInactiveFalse(Pageable pageable);

    Page<MisaWarehouse> findByIsInactiveTrue(Pageable pageable);

    @Query("SELECT w FROM MisaWarehouse w WHERE w.stockName LIKE %:query% OR w.stockCode LIKE %:query%")
    List<MisaWarehouse> searchByNameOrCode(@Param("query") String query);

    @Query("SELECT w FROM MisaWarehouse w WHERE w.branchId = :branchId")
    List<MisaWarehouse> findByBranchId(@Param("branchId") String branchId);

    @Query("SELECT DISTINCT w.branchCode FROM MisaWarehouse w WHERE w.branchCode IS NOT NULL ORDER BY w.branchCode")
    List<String> findDistinctBranchCodes();

    @Query("SELECT w FROM MisaWarehouse w WHERE w.lastSyncDate IS NULL OR w.lastSyncDate < :cutoff")
    List<MisaWarehouse> findWarehousesNeedingSync(@Param("cutoff") LocalDateTime cutoff);

    long countByIsInactiveFalse();

    long countByIsInactiveTrue();

    boolean existsByStockId(String stockId);

    boolean existsByStockCode(String stockCode);
}
