package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.PodQrCode;
import com.mirror.product.enums.QrCodeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PodQrCodeRepository extends JpaRepository<PodQrCode, String>, JpaSpecificationExecutor<PodQrCode> {

    Optional<PodQrCode> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<PodQrCode> findByPodId(String podId);

    Page<PodQrCode> findByPodIdAndIsDeletedFalse(String podId, Pageable pageable);

    List<PodQrCode> findByProductId(String productId);

    List<PodQrCode> findByStatus(QrCodeStatus status);

    @Query("SELECT q FROM PodQrCode q WHERE q.podId = :podId AND q.productId = :productId AND q.isDeleted = false")
    Optional<PodQrCode> findByPodIdAndProductId(@Param("podId") String podId, @Param("productId") String productId);

    @Query("SELECT q FROM PodQrCode q WHERE q.podId = :podId AND q.status = :status AND q.isDeleted = false")
    List<PodQrCode> findByPodIdAndStatus(@Param("podId") String podId, @Param("status") QrCodeStatus status);

    @Query("SELECT COUNT(q) FROM PodQrCode q WHERE q.podId = :podId AND q.isDeleted = false")
    long countByPodId(@Param("podId") String podId);

    @Query("SELECT q FROM PodQrCode q WHERE q.expiresAt IS NOT NULL AND q.expiresAt < :now AND q.status = 'ACTIVE'")
    List<PodQrCode> findExpiredQrCodes(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE PodQrCode q SET q.status = 'INACTIVE' WHERE q.podId = :podId")
    int deactivateAllByPodId(@Param("podId") String podId);

    @Modifying
    @Query("UPDATE PodQrCode q SET q.scanCount = q.scanCount + 1, q.lastScannedAt = :scannedAt WHERE q.id = :id")
    int incrementScanCount(@Param("id") String id, @Param("scannedAt") Instant scannedAt);

    @Query("SELECT q FROM PodQrCode q WHERE q.isDeleted = false ORDER BY q.scanCount DESC")
    Page<PodQrCode> findTopByScans(Pageable pageable);
}
