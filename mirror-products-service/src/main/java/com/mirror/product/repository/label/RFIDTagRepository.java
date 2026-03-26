package com.mirror.product.repository.label;

import com.mirror.product.entity.label.RFIDTag;
import com.mirror.product.enums.RFIDTagStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RFIDTagRepository extends JpaRepository<RFIDTag, String> {

    Optional<RFIDTag> findByIdAndIsDeletedFalse(String id);

    Optional<RFIDTag> findByEpc(String epc);

    Optional<RFIDTag> findByEpcAndIsDeletedFalse(String epc);

    boolean existsByEpc(String epc);

    List<RFIDTag> findByProductIdAndIsDeletedFalse(String productId);

    List<RFIDTag> findByProductIdAndStatusAndIsDeletedFalse(String productId, RFIDTagStatus status);

    Page<RFIDTag> findByStatusAndIsDeletedFalse(RFIDTagStatus status, Pageable pageable);

    List<RFIDTag> findByPrintJobIdAndIsDeletedFalse(String printJobId);

    @Query("SELECT t FROM RFIDTag t WHERE t.isDeleted = false " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:productId IS NULL OR t.productId = :productId) " +
           "ORDER BY t.encodedAt DESC")
    Page<RFIDTag> findWithFilters(
        @Param("status") RFIDTagStatus status,
        @Param("productId") String productId,
        Pageable pageable
    );

    @Query("SELECT COUNT(t) FROM RFIDTag t WHERE t.status = :status AND t.isDeleted = false")
    long countByStatus(@Param("status") RFIDTagStatus status);

    @Query("SELECT COUNT(t) FROM RFIDTag t WHERE t.isDeleted = false " +
           "AND t.encodedAt >= :startDate AND t.encodedAt <= :endDate")
    long countEncodedBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT SUM(t.scanCount) FROM RFIDTag t WHERE t.isDeleted = false")
    Long totalScanCount();
}
