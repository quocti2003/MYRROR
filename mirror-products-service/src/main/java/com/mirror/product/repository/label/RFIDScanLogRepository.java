package com.mirror.product.repository.label;

import com.mirror.product.entity.label.RFIDScanLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RFIDScanLogRepository extends JpaRepository<RFIDScanLog, String> {

    List<RFIDScanLog> findByEpcOrderByScannedAtDesc(String epc);

    Page<RFIDScanLog> findByEpcOrderByScannedAtDesc(String epc, Pageable pageable);

    List<RFIDScanLog> findByTagIdOrderByScannedAtDesc(String tagId);

    Page<RFIDScanLog> findByTagIdOrderByScannedAtDesc(String tagId, Pageable pageable);

    List<RFIDScanLog> findByProductIdOrderByScannedAtDesc(String productId);

    List<RFIDScanLog> findByDeviceIdOrderByScannedAtDesc(String deviceId);

    Page<RFIDScanLog> findByDeviceIdOrderByScannedAtDesc(String deviceId, Pageable pageable);

    @Query("SELECT l FROM RFIDScanLog l WHERE l.isDeleted = false " +
           "AND (:epc IS NULL OR l.epc = :epc) " +
           "AND (:deviceId IS NULL OR l.deviceId = :deviceId) " +
           "AND (:location IS NULL OR l.location = :location) " +
           "AND (:startDate IS NULL OR l.scannedAt >= :startDate) " +
           "AND (:endDate IS NULL OR l.scannedAt <= :endDate) " +
           "ORDER BY l.scannedAt DESC")
    Page<RFIDScanLog> findWithFilters(
        @Param("epc") String epc,
        @Param("deviceId") String deviceId,
        @Param("location") String location,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    @Query("SELECT COUNT(l) FROM RFIDScanLog l WHERE l.isDeleted = false " +
           "AND l.scannedAt >= :startDate AND l.scannedAt <= :endDate")
    long countScansBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(DISTINCT l.epc) FROM RFIDScanLog l WHERE l.isDeleted = false " +
           "AND l.scannedAt >= :startDate AND l.scannedAt <= :endDate")
    long countUniqueTagsScannedBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT l.deviceId, COUNT(l) FROM RFIDScanLog l WHERE l.isDeleted = false " +
           "AND l.scannedAt >= :startDate AND l.scannedAt <= :endDate " +
           "GROUP BY l.deviceId ORDER BY COUNT(l) DESC")
    List<Object[]> countByDeviceBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT l.location, COUNT(l) FROM RFIDScanLog l WHERE l.isDeleted = false " +
           "AND l.location IS NOT NULL " +
           "AND l.scannedAt >= :startDate AND l.scannedAt <= :endDate " +
           "GROUP BY l.location ORDER BY COUNT(l) DESC")
    List<Object[]> countByLocationBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
