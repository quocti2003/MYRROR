package com.mirror.product.repository.label;

import com.mirror.product.entity.label.PrintJob;
import com.mirror.product.enums.PrintJobStatus;
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
public interface PrintJobRepository extends JpaRepository<PrintJob, String> {

    Optional<PrintJob> findByIdAndIsDeletedFalse(String id);

    List<PrintJob> findByStatusAndIsDeletedFalse(PrintJobStatus status);

    Page<PrintJob> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<PrintJob> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(PrintJobStatus status, Pageable pageable);

    @Query("SELECT j FROM PrintJob j WHERE j.isDeleted = false " +
           "AND (:status IS NULL OR j.status = :status) " +
           "AND (:templateId IS NULL OR :templateId = '' OR j.templateId = :templateId) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR j.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR j.createdAt <= :endDate) " +
           "ORDER BY j.createdAt DESC")
    Page<PrintJob> findWithFilters(
        @Param("status") PrintJobStatus status,
        @Param("templateId") String templateId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    List<PrintJob> findByTemplateIdAndIsDeletedFalse(String templateId);

    @Query("SELECT COUNT(j) FROM PrintJob j WHERE j.status = :status AND j.isDeleted = false")
    long countByStatus(@Param("status") PrintJobStatus status);

    @Query("SELECT SUM(j.printedLabels) FROM PrintJob j WHERE j.isDeleted = false " +
           "AND j.createdAt >= :startDate AND j.createdAt <= :endDate")
    Long sumPrintedLabelsBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT SUM(j.failedLabels) FROM PrintJob j WHERE j.isDeleted = false " +
           "AND j.createdAt >= :startDate AND j.createdAt <= :endDate")
    Long sumFailedLabelsBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
