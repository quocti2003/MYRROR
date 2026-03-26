package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.PodAttribution;
import com.mirror.product.enums.AttributionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PodAttributionRepository extends JpaRepository<PodAttribution, String>, JpaSpecificationExecutor<PodAttribution> {

    List<PodAttribution> findByOrderId(String orderId);

    Optional<PodAttribution> findByOrderIdAndPodId(String orderId, String podId);

    List<PodAttribution> findByPodId(String podId);

    List<PodAttribution> findByPartnerId(String partnerId);

    Page<PodAttribution> findByPartnerIdAndIsDeletedFalse(String partnerId, Pageable pageable);

    List<PodAttribution> findByStatus(AttributionStatus status);

    List<PodAttribution> findByCommissionId(String commissionId);

    @Query("SELECT a FROM PodAttribution a WHERE a.partnerId = :partnerId AND a.orderPlacedAt BETWEEN :start AND :end AND a.isDeleted = false")
    List<PodAttribution> findByPartnerIdAndDateRange(
        @Param("partnerId") String partnerId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    @Query("SELECT a FROM PodAttribution a WHERE a.partnerId = :partnerId AND a.status = :status AND a.commissionId IS NULL AND a.isDeleted = false")
    List<PodAttribution> findUnprocessedAttributions(
        @Param("partnerId") String partnerId,
        @Param("status") AttributionStatus status
    );

    @Query("SELECT COUNT(a) FROM PodAttribution a WHERE a.partnerId = :partnerId AND a.isDeleted = false")
    long countByPartnerId(@Param("partnerId") String partnerId);

    @Query("SELECT COUNT(a) FROM PodAttribution a WHERE a.podId = :podId AND a.isDeleted = false")
    long countByPodId(@Param("podId") String podId);

    @Query("SELECT COALESCE(SUM(a.attributedAmount), 0) FROM PodAttribution a WHERE a.partnerId = :partnerId AND a.status = 'CONFIRMED' AND a.isDeleted = false")
    BigDecimal sumAttributedAmountByPartnerId(@Param("partnerId") String partnerId);

    @Query("SELECT COALESCE(SUM(a.attributedAmount), 0) FROM PodAttribution a WHERE a.partnerId = :partnerId AND a.orderPlacedAt BETWEEN :start AND :end AND a.status = 'CONFIRMED' AND a.isDeleted = false")
    BigDecimal sumAttributedAmountByPartnerIdAndDateRange(
        @Param("partnerId") String partnerId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    @Query("SELECT COUNT(a) FROM PodAttribution a WHERE a.partnerId = :partnerId AND a.orderPlacedAt BETWEEN :start AND :end AND a.status = 'CONFIRMED' AND a.isDeleted = false")
    long countByPartnerIdAndDateRange(
        @Param("partnerId") String partnerId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    /**
     * Calculate conversion rate (attributions / scans) for a partner
     */
    @Query("SELECT CAST(COUNT(a) AS double) / NULLIF((SELECT COUNT(s) FROM PodQrScan s WHERE s.partnerId = :partnerId AND s.isDeleted = false), 0) " +
           "FROM PodAttribution a WHERE a.partnerId = :partnerId AND a.isDeleted = false")
    Double calculateConversionRateByPartnerId(@Param("partnerId") String partnerId);

    /**
     * Find attributions within date range (for admin dashboard)
     */
    @Query("SELECT a FROM PodAttribution a WHERE a.orderPlacedAt BETWEEN :start AND :end AND a.isDeleted = false")
    List<PodAttribution> findByDateRange(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Calculate overall conversion rate (attributions / scans) across all partners
     */
    @Query("SELECT CAST(COUNT(a) AS double) / NULLIF((SELECT COUNT(s) FROM PodQrScan s WHERE s.isDeleted = false), 0) " +
           "FROM PodAttribution a WHERE a.isDeleted = false")
    Double calculateOverallConversionRate();

    /**
     * Sum attributed amount by POD ID
     */
    @Query("SELECT COALESCE(SUM(a.attributedAmount), 0) FROM PodAttribution a WHERE a.podId = :podId AND a.status = 'CONFIRMED' AND a.isDeleted = false")
    BigDecimal sumAttributedAmountByPodId(@Param("podId") String podId);
}
