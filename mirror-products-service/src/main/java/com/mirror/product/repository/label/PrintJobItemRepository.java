package com.mirror.product.repository.label;

import com.mirror.product.entity.label.PrintJobItem;
import com.mirror.product.enums.PrintJobItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrintJobItemRepository extends JpaRepository<PrintJobItem, String> {

    Optional<PrintJobItem> findByIdAndIsDeletedFalse(String id);

    List<PrintJobItem> findByPrintJobIdAndIsDeletedFalse(String printJobId);

    List<PrintJobItem> findByPrintJobIdAndStatusAndIsDeletedFalse(String printJobId, PrintJobItemStatus status);

    Page<PrintJobItem> findByPrintJobIdAndIsDeletedFalse(String printJobId, Pageable pageable);

    List<PrintJobItem> findByProductIdAndIsDeletedFalse(String productId);

    Optional<PrintJobItem> findByEpcAndIsDeletedFalse(String epc);

    @Query("SELECT COUNT(i) FROM PrintJobItem i WHERE i.printJobId = :printJobId " +
           "AND i.status = :status AND i.isDeleted = false")
    long countByPrintJobIdAndStatus(@Param("printJobId") String printJobId, @Param("status") PrintJobItemStatus status);

    @Query("SELECT i FROM PrintJobItem i WHERE i.printJobId = :printJobId " +
           "AND i.status = 'FAILED' AND i.isDeleted = false")
    List<PrintJobItem> findFailedItems(@Param("printJobId") String printJobId);
}
