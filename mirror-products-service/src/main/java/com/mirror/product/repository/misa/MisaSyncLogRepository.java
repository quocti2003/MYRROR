package com.mirror.product.repository.misa;

import com.mirror.product.entity.misa.MisaSyncLog;
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
public interface MisaSyncLogRepository extends JpaRepository<MisaSyncLog, Long> {

    List<MisaSyncLog> findBySyncType(MisaSyncLog.SyncType syncType);

    List<MisaSyncLog> findByStatus(MisaSyncLog.SyncStatus status);

    Optional<MisaSyncLog> findTopBySyncTypeOrderByCreatedAtDesc(MisaSyncLog.SyncType syncType);

    @Query("SELECT m FROM MisaSyncLog m WHERE m.syncType = :syncType AND m.status = :status ORDER BY m.createdAt DESC")
    List<MisaSyncLog> findBySyncTypeAndStatus(@Param("syncType") MisaSyncLog.SyncType syncType,
                                              @Param("status") MisaSyncLog.SyncStatus status);

    @Query("SELECT m FROM MisaSyncLog m WHERE m.createdAt BETWEEN :startDate AND :endDate ORDER BY m.createdAt DESC")
    List<MisaSyncLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT m FROM MisaSyncLog m WHERE m.status = 'FAILED' ORDER BY m.createdAt DESC")
    List<MisaSyncLog> findFailedSyncs();

    Page<MisaSyncLog> findByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(m) FROM MisaSyncLog m WHERE m.status = 'COMPLETED' AND m.createdAt > :since")
    Long countSuccessfulSyncsSince(@Param("since") LocalDateTime since);
}
