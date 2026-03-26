package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.PodCommission;
import com.mirror.product.enums.CommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PodCommissionRepository extends JpaRepository<PodCommission, String>, JpaSpecificationExecutor<PodCommission> {

    List<PodCommission> findByPartnerId(String partnerId);

    Page<PodCommission> findByPartnerIdAndIsDeletedFalse(String partnerId, Pageable pageable);

    List<PodCommission> findByStatus(CommissionStatus status);

    Page<PodCommission> findByStatusAndIsDeletedFalse(CommissionStatus status, Pageable pageable);

    @Query("SELECT c FROM PodCommission c WHERE c.partnerId = :partnerId AND c.periodStart = :periodStart AND c.periodEnd = :periodEnd")
    Optional<PodCommission> findByPartnerIdAndPeriod(
        @Param("partnerId") String partnerId,
        @Param("periodStart") LocalDate periodStart,
        @Param("periodEnd") LocalDate periodEnd
    );

    @Query("SELECT c FROM PodCommission c WHERE c.partnerId = :partnerId AND c.status = :status AND c.isDeleted = false ORDER BY c.periodStart DESC")
    List<PodCommission> findByPartnerIdAndStatus(
        @Param("partnerId") String partnerId,
        @Param("status") CommissionStatus status
    );

    @Query("SELECT c FROM PodCommission c WHERE c.periodStart >= :start AND c.periodEnd <= :end AND c.isDeleted = false")
    List<PodCommission> findByPeriodRange(
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    @Query("SELECT COALESCE(SUM(c.finalAmount), 0) FROM PodCommission c WHERE c.partnerId = :partnerId AND c.status = 'PAID' AND c.isDeleted = false")
    BigDecimal sumPaidAmountByPartnerId(@Param("partnerId") String partnerId);

    @Query("SELECT COALESCE(SUM(c.finalAmount), 0) FROM PodCommission c WHERE c.partnerId = :partnerId AND c.status = 'PENDING' AND c.isDeleted = false")
    BigDecimal sumPendingAmountByPartnerId(@Param("partnerId") String partnerId);

    @Query("SELECT COALESCE(SUM(c.finalAmount), 0) FROM PodCommission c WHERE c.status = 'PENDING' AND c.isDeleted = false")
    BigDecimal sumTotalPendingAmount();

    @Query("SELECT COUNT(c) FROM PodCommission c WHERE c.partnerId = :partnerId AND c.isDeleted = false")
    long countByPartnerId(@Param("partnerId") String partnerId);

    @Query("SELECT COUNT(c) FROM PodCommission c WHERE c.status = :status AND c.isDeleted = false")
    long countByStatus(@Param("status") CommissionStatus status);

    @Query("SELECT c FROM PodCommission c WHERE c.isDeleted = false ORDER BY c.periodStart DESC")
    Page<PodCommission> findAllActive(Pageable pageable);

    /**
     * Find commissions pending approval that are older than a certain date
     */
    @Query("SELECT c FROM PodCommission c WHERE c.status = 'PENDING' AND c.periodEnd < :beforeDate AND c.isDeleted = false")
    List<PodCommission> findPendingCommissionsBeforeDate(@Param("beforeDate") LocalDate beforeDate);
}
