package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.PodQrScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PodQrScanRepository extends JpaRepository<PodQrScan, String>, JpaSpecificationExecutor<PodQrScan> {

    List<PodQrScan> findByQrCodeId(String qrCodeId);

    List<PodQrScan> findByPodId(String podId);

    List<PodQrScan> findByPartnerId(String partnerId);

    Page<PodQrScan> findByPartnerIdAndIsDeletedFalse(String partnerId, Pageable pageable);

    List<PodQrScan> findBySessionId(String sessionId);

    List<PodQrScan> findByUserId(Long userId);

    @Query("SELECT s FROM PodQrScan s WHERE s.partnerId = :partnerId AND s.scannedAt BETWEEN :start AND :end AND s.isDeleted = false")
    List<PodQrScan> findByPartnerIdAndDateRange(
        @Param("partnerId") String partnerId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    @Query("SELECT s FROM PodQrScan s WHERE s.podId = :podId AND s.scannedAt BETWEEN :start AND :end AND s.isDeleted = false")
    List<PodQrScan> findByPodIdAndDateRange(
        @Param("podId") String podId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    @Query("SELECT COUNT(s) FROM PodQrScan s WHERE s.partnerId = :partnerId AND s.isDeleted = false")
    long countByPartnerId(@Param("partnerId") String partnerId);

    @Query("SELECT COUNT(s) FROM PodQrScan s WHERE s.podId = :podId AND s.isDeleted = false")
    long countByPodId(@Param("podId") String podId);

    @Query("SELECT COUNT(s) FROM PodQrScan s WHERE s.qrCodeId = :qrCodeId AND s.isDeleted = false")
    long countByQrCodeId(@Param("qrCodeId") String qrCodeId);

    @Query("SELECT COUNT(s) FROM PodQrScan s WHERE s.partnerId = :partnerId AND s.scannedAt BETWEEN :start AND :end AND s.isDeleted = false")
    long countByPartnerIdAndDateRange(
        @Param("partnerId") String partnerId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    @Query("SELECT COUNT(s) FROM PodQrScan s WHERE s.isUnique = true AND s.partnerId = :partnerId AND s.scannedAt BETWEEN :start AND :end AND s.isDeleted = false")
    long countUniqueByPartnerIdAndDateRange(
        @Param("partnerId") String partnerId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    /**
     * Check for duplicate scan within time window (for deduplication)
     */
    @Query("SELECT s FROM PodQrScan s WHERE s.qrCodeId = :qrCodeId AND s.sessionId = :sessionId AND s.scannedAt > :since AND s.isDeleted = false")
    Optional<PodQrScan> findRecentScanBySessionAndQrCode(
        @Param("qrCodeId") String qrCodeId,
        @Param("sessionId") String sessionId,
        @Param("since") Instant since
    );

    /**
     * Find scans by user within attribution window
     */
    @Query("SELECT s FROM PodQrScan s WHERE s.userId = :userId AND s.scannedAt > :since AND s.isDeleted = false ORDER BY s.scannedAt DESC")
    List<PodQrScan> findRecentScansByUserId(@Param("userId") Long userId, @Param("since") Instant since);

    /**
     * Find scans by session within attribution window
     */
    @Query("SELECT s FROM PodQrScan s WHERE s.sessionId = :sessionId AND s.scannedAt > :since AND s.isDeleted = false ORDER BY s.scannedAt DESC")
    List<PodQrScan> findRecentScansBySessionId(@Param("sessionId") String sessionId, @Param("since") Instant since);

    /**
     * Count total scans (for admin dashboard)
     */
    @Query("SELECT COUNT(s) FROM PodQrScan s WHERE s.isDeleted = false")
    long countTotal();

    /**
     * Count scans within date range (for admin dashboard)
     */
    @Query("SELECT COUNT(s) FROM PodQrScan s WHERE s.scannedAt BETWEEN :start AND :end AND s.isDeleted = false")
    long countByDateRange(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Check if scan record already exists for deduplication on login
     */
    boolean existsByQrCodeIdAndUserIdAndScannedAtAndIsDeletedFalse(
        String qrCodeId, Long userId, Instant scannedAt
    );
}
