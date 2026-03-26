package com.mirror.product.repository.misa;

import com.mirror.product.entity.misa.MisaInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MisaInvoiceRepository extends JpaRepository<MisaInvoice, Long> {

    Optional<MisaInvoice> findByInvoiceId(String invoiceId);

    Optional<MisaInvoice> findByInvoiceCode(String invoiceCode);

    List<MisaInvoice> findByCustomerId(String customerId);

    Page<MisaInvoice> findByCustomerId(String customerId, Pageable pageable);

    Page<MisaInvoice> findByInvoiceDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    List<MisaInvoice> findByInvoiceDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT i FROM MisaInvoice i WHERE i.lastSyncDate IS NULL OR i.lastSyncDate < :cutoffTime")
    List<MisaInvoice> findInvoicesNeedingSync(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT SUM(i.finalAmount) FROM MisaInvoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(i) FROM MisaInvoice i WHERE i.paymentStatus = :paymentStatus")
    Long countByPaymentStatus(@Param("paymentStatus") Integer paymentStatus);

    @Query("SELECT i FROM MisaInvoice i WHERE i.debtAmount > 0")
    List<MisaInvoice> findInvoicesWithDebt();

    @Query("SELECT DISTINCT i.branchName FROM MisaInvoice i WHERE i.branchName IS NOT NULL ORDER BY i.branchName")
    List<String> findDistinctBranches();

    @Query("SELECT i FROM MisaInvoice i WHERE i.invoiceDate >= :startDate ORDER BY i.finalAmount DESC")
    Page<MisaInvoice> findTopInvoicesSince(@Param("startDate") LocalDateTime startDate, Pageable pageable);
}
