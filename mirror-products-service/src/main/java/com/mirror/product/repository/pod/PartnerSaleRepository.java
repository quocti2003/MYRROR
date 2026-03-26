package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.PartnerSale;
import com.mirror.product.enums.PartnerSaleStatus;
import com.mirror.product.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerSaleRepository extends BaseRepository<PartnerSale, String>,
        JpaSpecificationExecutor<PartnerSale> {

    Page<PartnerSale> findByPartnerIdAndIsDeletedFalseOrderBySoldAtDesc(
        String partnerId, Pageable pageable);

    Optional<PartnerSale> findByIdAndPartnerIdAndIsDeletedFalse(String id, String partnerId);

    List<PartnerSale> findByPartnerIdAndIsDeletedFalse(String partnerId);

    Page<PartnerSale> findByPartnerIdAndStatusAndIsDeletedFalse(
        String partnerId, PartnerSaleStatus status, Pageable pageable);

    @Query("SELECT COUNT(ps) FROM PartnerSale ps WHERE ps.partnerId = :partnerId " +
           "AND ps.soldAt BETWEEN :start AND :end AND ps.status != :excludeStatus AND ps.isDeleted = false")
    long countSalesInPeriod(@Param("partnerId") String partnerId,
                            @Param("start") Instant start, @Param("end") Instant end,
                            @Param("excludeStatus") PartnerSaleStatus excludeStatus);

    @Query("SELECT COALESCE(SUM(ps.totalAmount), 0) FROM PartnerSale ps " +
           "WHERE ps.partnerId = :partnerId AND ps.soldAt BETWEEN :start AND :end " +
           "AND ps.status != :excludeStatus AND ps.isDeleted = false")
    BigDecimal calculateRevenueInPeriod(@Param("partnerId") String partnerId,
                                        @Param("start") Instant start, @Param("end") Instant end,
                                        @Param("excludeStatus") PartnerSaleStatus excludeStatus);

    @Query("SELECT COALESCE(SUM(ps.profitAmount), 0) FROM PartnerSale ps " +
           "WHERE ps.partnerId = :partnerId AND ps.soldAt BETWEEN :start AND :end " +
           "AND ps.status != :excludeStatus AND ps.isDeleted = false")
    BigDecimal calculateProfitInPeriod(@Param("partnerId") String partnerId,
                                       @Param("start") Instant start, @Param("end") Instant end,
                                       @Param("excludeStatus") PartnerSaleStatus excludeStatus);

    @Query("SELECT COALESCE(AVG(ps.profitMarginPercent), 0) FROM PartnerSale ps " +
           "WHERE ps.partnerId = :partnerId AND ps.soldAt BETWEEN :start AND :end " +
           "AND ps.status != :excludeStatus AND ps.isDeleted = false")
    BigDecimal calculateAvgMarginInPeriod(@Param("partnerId") String partnerId,
                                           @Param("start") Instant start, @Param("end") Instant end,
                                           @Param("excludeStatus") PartnerSaleStatus excludeStatus);
}
