package com.mirror.product.repository;

import com.mirror.product.entity.ProductionOrder;
import com.mirror.product.enums.ProductionOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductionOrderRepository extends BaseRepository<ProductionOrder, String> {

    @Override
    @Query("SELECT po FROM ProductionOrder po WHERE po.id = :id AND po.isActive = true AND po.isDeleted = false")
    Optional<ProductionOrder> findActiveById(@Param("id") String id);

    @Query("SELECT po FROM ProductionOrder po " +
           "LEFT JOIN FETCH po.stages s " +
           "WHERE po.id = :id AND po.isActive = true AND po.isDeleted = false " +
           "ORDER BY s.stageOrder ASC")
    Optional<ProductionOrder> findActiveByIdWithStages(@Param("id") String id);

    @Query("SELECT po FROM ProductionOrder po " +
           "LEFT JOIN FETCH po.productionPlan pp " +
           "WHERE po.id = :id AND po.isActive = true AND po.isDeleted = false")
    Optional<ProductionOrder> findActiveByIdWithPlan(@Param("id") String id);

    @Query("SELECT po FROM ProductionOrder po WHERE po.orderNumber = :orderNumber AND po.isActive = true AND po.isDeleted = false")
    Optional<ProductionOrder> findActiveByOrderNumber(@Param("orderNumber") String orderNumber);

    @Query("SELECT po FROM ProductionOrder po WHERE po.productionPlan.id = :planId AND po.isActive = true AND po.isDeleted = false ORDER BY po.orderNumber")
    List<ProductionOrder> findActiveByProductionPlanId(@Param("planId") String planId);

    @Query("SELECT po FROM ProductionOrder po WHERE po.status = :status AND po.isActive = true AND po.isDeleted = false")
    List<ProductionOrder> findActiveByStatus(@Param("status") ProductionOrderStatus status);

    @Query("SELECT po FROM ProductionOrder po WHERE po.currentHolderId = :vendorId AND po.isActive = true AND po.isDeleted = false")
    List<ProductionOrder> findActiveByCurrentHolderId(@Param("vendorId") String vendorId);

    @Query("SELECT po FROM ProductionOrder po WHERE po.collectionPlanItemId = :itemId AND po.isActive = true AND po.isDeleted = false")
    Optional<ProductionOrder> findActiveByCollectionPlanItemId(@Param("itemId") UUID itemId);

    @Query("SELECT po FROM ProductionOrder po WHERE po.jtrc.id = :jtrcId AND po.isActive = true AND po.isDeleted = false")
    List<ProductionOrder> findActiveByJtrcId(@Param("jtrcId") String jtrcId);

    // Search with filters
    @Query("SELECT po FROM ProductionOrder po WHERE po.isActive = true AND po.isDeleted = false " +
           "AND (LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(po.notes) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ProductionOrder> searchActive(@Param("search") String search, Pageable pageable);

    @Query("SELECT po FROM ProductionOrder po WHERE po.isActive = true AND po.isDeleted = false " +
           "AND (:planId IS NULL OR po.productionPlan.id = :planId) " +
           "AND (:status IS NULL OR po.status = :status) " +
           "AND (:vendorId IS NULL OR po.currentHolderId = :vendorId)")
    Page<ProductionOrder> findAllActiveWithFilters(
        @Param("planId") String planId,
        @Param("status") ProductionOrderStatus status,
        @Param("vendorId") String vendorId,
        Pageable pageable
    );

    // Date-based queries
    @Query("SELECT po FROM ProductionOrder po WHERE po.estimatedCompletionDate BETWEEN :startDate AND :endDate " +
           "AND po.isActive = true AND po.isDeleted = false")
    List<ProductionOrder> findActiveByEstimatedCompletionDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT po FROM ProductionOrder po WHERE po.estimatedCompletionDate < :date " +
           "AND po.status NOT IN ('COMPLETED', 'CANCELLED') " +
           "AND po.isActive = true AND po.isDeleted = false")
    List<ProductionOrder> findOverdueOrders(@Param("date") LocalDate date);

    // Counting queries
    @Query("SELECT COUNT(po) FROM ProductionOrder po WHERE po.productionPlan.id = :planId " +
           "AND po.isActive = true AND po.isDeleted = false")
    int countByProductionPlanId(@Param("planId") String planId);

    @Query("SELECT COUNT(po) FROM ProductionOrder po WHERE po.productionPlan.id = :planId " +
           "AND po.status = :status AND po.isActive = true AND po.isDeleted = false")
    int countByProductionPlanIdAndStatus(@Param("planId") String planId, @Param("status") ProductionOrderStatus status);

    @Query("SELECT COUNT(po) FROM ProductionOrder po WHERE po.currentHolderId = :vendorId " +
           "AND po.status = 'IN_PROGRESS' AND po.isActive = true AND po.isDeleted = false")
    int countInProgressByVendorId(@Param("vendorId") String vendorId);

    // Order number generation
    @Query("SELECT po.orderNumber FROM ProductionOrder po WHERE po.orderNumber LIKE :prefix% " +
           "ORDER BY po.orderNumber DESC LIMIT 1")
    Optional<String> findLastOrderNumberWithPrefix(@Param("prefix") String prefix);

    // Existence checks
    @Query("SELECT COUNT(po) > 0 FROM ProductionOrder po WHERE po.orderNumber = :orderNumber")
    boolean existsByOrderNumber(@Param("orderNumber") String orderNumber);

    @Query("SELECT COUNT(po) > 0 FROM ProductionOrder po WHERE po.productionPlan.id = :planId " +
           "AND po.isActive = true AND po.isDeleted = false")
    boolean existsByProductionPlanId(@Param("planId") String planId);
}
