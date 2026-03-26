package com.mirror.product.repository;

import com.mirror.product.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for PurchaseOrder - Purchase orders to vendors
 * Migrated from mirror-mrp-service
 */
@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    List<PurchaseOrder> findByStatus(String status);

    List<PurchaseOrder> findByVendorId(String vendorId);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.expectedDeliveryDate BETWEEN :startDate AND :endDate")
    List<PurchaseOrder> findByExpectedDeliveryDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT po FROM PurchaseOrder po WHERE po.vendor.id = :vendorId AND po.status = :status")
    List<PurchaseOrder> findByVendorIdAndStatus(@Param("vendorId") String vendorId, @Param("status") String status);
}
