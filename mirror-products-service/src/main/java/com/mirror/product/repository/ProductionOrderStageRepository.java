package com.mirror.product.repository;

import com.mirror.product.entity.ProductionOrderStage;
import com.mirror.product.enums.PartnerCapabilityType;
import com.mirror.product.enums.ProductionOrderStageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionOrderStageRepository extends BaseRepository<ProductionOrderStage, String> {

    @Override
    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.id = :id AND pos.isActive = true AND pos.isDeleted = false")
    Optional<ProductionOrderStage> findActiveById(@Param("id") String id);

    @Query("SELECT pos FROM ProductionOrderStage pos " +
           "LEFT JOIN FETCH pos.productionOrder po " +
           "WHERE pos.id = :id AND pos.isActive = true AND pos.isDeleted = false")
    Optional<ProductionOrderStage> findActiveByIdWithOrder(@Param("id") String id);

    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.productionOrder.id = :orderId " +
           "AND pos.isActive = true AND pos.isDeleted = false ORDER BY pos.stageOrder ASC")
    List<ProductionOrderStage> findActiveByProductionOrderId(@Param("orderId") String orderId);

    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.productionOrder.id = :orderId " +
           "AND pos.stageOrder = :stageOrder AND pos.isActive = true AND pos.isDeleted = false")
    Optional<ProductionOrderStage> findActiveByOrderIdAndStageOrder(
        @Param("orderId") String orderId,
        @Param("stageOrder") Integer stageOrder
    );

    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.assignedVendor.id = :vendorId " +
           "AND pos.isActive = true AND pos.isDeleted = false")
    List<ProductionOrderStage> findActiveByAssignedVendorId(@Param("vendorId") String vendorId);

    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.status = :status " +
           "AND pos.isActive = true AND pos.isDeleted = false")
    List<ProductionOrderStage> findActiveByStatus(@Param("status") ProductionOrderStageStatus status);

    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.assignedVendor.id = :vendorId " +
           "AND pos.status = :status AND pos.isActive = true AND pos.isDeleted = false")
    List<ProductionOrderStage> findActiveByVendorIdAndStatus(
        @Param("vendorId") String vendorId,
        @Param("status") ProductionOrderStageStatus status
    );

    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.requiredCapability = :capability " +
           "AND pos.status = :status AND pos.isActive = true AND pos.isDeleted = false")
    List<ProductionOrderStage> findActiveByCapabilityAndStatus(
        @Param("capability") PartnerCapabilityType capability,
        @Param("status") ProductionOrderStageStatus status
    );

    // Find next stage after a given stage order for an order
    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.productionOrder.id = :orderId " +
           "AND pos.stageOrder > :currentOrder AND pos.isActive = true AND pos.isDeleted = false " +
           "ORDER BY pos.stageOrder ASC LIMIT 1")
    Optional<ProductionOrderStage> findNextStage(
        @Param("orderId") String orderId,
        @Param("currentOrder") Integer currentOrder
    );

    // Find previous completed stage before a given stage order for an order
    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.productionOrder.id = :orderId " +
           "AND pos.stageOrder < :currentOrder AND pos.status IN ('COMPLETED', 'SKIPPED') " +
           "AND pos.isActive = true AND pos.isDeleted = false " +
           "ORDER BY pos.stageOrder DESC LIMIT 1")
    Optional<ProductionOrderStage> findPreviousStage(
        @Param("orderId") String orderId,
        @Param("currentOrder") Integer currentOrder
    );

    // Find stages with pagination and filters
    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.isActive = true AND pos.isDeleted = false " +
           "AND (:vendorId IS NULL OR pos.assignedVendor.id = :vendorId) " +
           "AND (:status IS NULL OR pos.status = :status) " +
           "AND (:capability IS NULL OR pos.requiredCapability = :capability)")
    Page<ProductionOrderStage> findAllActiveWithFilters(
        @Param("vendorId") String vendorId,
        @Param("status") ProductionOrderStageStatus status,
        @Param("capability") PartnerCapabilityType capability,
        Pageable pageable
    );

    // Date-based queries
    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.plannedEndDate < :date " +
           "AND pos.status NOT IN ('COMPLETED', 'SKIPPED') " +
           "AND pos.isActive = true AND pos.isDeleted = false")
    List<ProductionOrderStage> findOverdueStages(@Param("date") LocalDate date);

    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.plannedStartDate BETWEEN :startDate AND :endDate " +
           "AND pos.isActive = true AND pos.isDeleted = false")
    List<ProductionOrderStage> findActiveByPlannedStartDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // Counting queries
    @Query("SELECT COUNT(pos) FROM ProductionOrderStage pos WHERE pos.productionOrder.id = :orderId " +
           "AND pos.status = :status AND pos.isActive = true AND pos.isDeleted = false")
    int countByOrderIdAndStatus(@Param("orderId") String orderId, @Param("status") ProductionOrderStageStatus status);

    @Query("SELECT COUNT(pos) FROM ProductionOrderStage pos WHERE pos.assignedVendor.id = :vendorId " +
           "AND pos.status = 'IN_PROGRESS' AND pos.isActive = true AND pos.isDeleted = false")
    int countInProgressByVendorId(@Param("vendorId") String vendorId);

    // Find stages ready for a vendor (by capability, ready status, unassigned or assigned to vendor)
    @Query("SELECT pos FROM ProductionOrderStage pos WHERE pos.requiredCapability = :capability " +
           "AND pos.status = 'READY' AND (pos.assignedVendor IS NULL OR pos.assignedVendor.id = :vendorId) " +
           "AND pos.isActive = true AND pos.isDeleted = false")
    List<ProductionOrderStage> findAvailableStagesForVendor(
        @Param("vendorId") String vendorId,
        @Param("capability") PartnerCapabilityType capability
    );

    // Sourcing Report: Find all stages for a production plan with vendor assigned
    @Query("SELECT pos FROM ProductionOrderStage pos " +
           "LEFT JOIN FETCH pos.productionOrder po " +
           "LEFT JOIN FETCH pos.assignedVendor v " +
           "WHERE po.productionPlan.id = :planId " +
           "AND pos.assignedVendor IS NOT NULL " +
           "AND pos.isActive = true AND pos.isDeleted = false " +
           "ORDER BY v.id, po.orderNumber, pos.stageOrder")
    List<ProductionOrderStage> findStagesForSourcingReport(@Param("planId") String planId);

    // Sourcing Report: Find stages by plan and vendor
    @Query("SELECT pos FROM ProductionOrderStage pos " +
           "LEFT JOIN FETCH pos.productionOrder po " +
           "LEFT JOIN FETCH po.jtrc j " +
           "WHERE po.productionPlan.id = :planId " +
           "AND pos.assignedVendor.id = :vendorId " +
           "AND pos.isActive = true AND pos.isDeleted = false " +
           "ORDER BY po.orderNumber, pos.stageOrder")
    List<ProductionOrderStage> findStagesByPlanAndVendor(
        @Param("planId") String planId,
        @Param("vendorId") String vendorId
    );

    // Get distinct vendor IDs for a production plan
    @Query("SELECT DISTINCT pos.assignedVendor.id FROM ProductionOrderStage pos " +
           "WHERE pos.productionOrder.productionPlan.id = :planId " +
           "AND pos.assignedVendor IS NOT NULL " +
           "AND pos.isActive = true AND pos.isDeleted = false")
    List<String> findDistinctVendorIdsByPlanId(@Param("planId") String planId);
}
