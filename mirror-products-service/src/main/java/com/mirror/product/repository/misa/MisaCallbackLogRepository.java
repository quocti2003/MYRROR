package com.mirror.product.repository.misa;

import com.mirror.product.entity.misa.MisaCallbackLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MisaCallbackLogRepository extends JpaRepository<MisaCallbackLog, Long> {

    /**
     * Find callbacks by data type
     */
    Page<MisaCallbackLog> findByDataType(Integer dataType, Pageable pageable);

    /**
     * Find callbacks by original reference ID
     */
    List<MisaCallbackLog> findByOrgRefId(String orgRefId);

    /**
     * Find callbacks by MISA reference ID
     */
    Optional<MisaCallbackLog> findByMisaRefId(String misaRefId);

    /**
     * Find unprocessed callbacks
     */
    List<MisaCallbackLog> findByProcessedFalseOrderByReceivedAtAsc();

    /**
     * Find failed callbacks (MISA reported error)
     */
    Page<MisaCallbackLog> findByMisaSuccessFalse(Pageable pageable);

    /**
     * Find callbacks with invalid signatures
     */
    Page<MisaCallbackLog> findBySignatureValidFalse(Pageable pageable);

    /**
     * Find callbacks within date range
     */
    Page<MisaCallbackLog> findByReceivedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Count callbacks by data type
     */
    @Query("SELECT c.dataType, c.dataTypeName, COUNT(c) FROM MisaCallbackLog c " +
           "WHERE c.receivedAt >= :since GROUP BY c.dataType, c.dataTypeName")
    List<Object[]> countByDataTypeSince(LocalDateTime since);

    /**
     * Count failed callbacks
     */
    long countByMisaSuccessFalseAndReceivedAtAfter(LocalDateTime since);

    /**
     * Count successful callbacks
     */
    long countByMisaSuccessTrueAndReceivedAtAfter(LocalDateTime since);

    /**
     * Find recent callbacks
     */
    List<MisaCallbackLog> findTop50ByOrderByReceivedAtDesc();

    /**
     * Find callbacks by voucher type
     */
    Page<MisaCallbackLog> findByVoucherType(Integer voucherType, Pageable pageable);
}
