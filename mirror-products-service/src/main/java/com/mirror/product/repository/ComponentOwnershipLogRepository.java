package com.mirror.product.repository;

import com.mirror.product.entity.ComponentOwnershipLog;
import com.mirror.product.enums.HandoffStatus;
import com.mirror.product.enums.HandoffType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ComponentOwnershipLog entity.
 * Provides methods to query component ownership history and track handoffs.
 */
@Repository
public interface ComponentOwnershipLogRepository extends JpaRepository<ComponentOwnershipLog, String>,
        JpaSpecificationExecutor<ComponentOwnershipLog> {

    /**
     * Find all ownership logs for a production order, ordered by initiated time
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.productionOrder.id = :orderId " +
           "AND col.isDeleted = false " +
           "ORDER BY col.initiatedAt DESC")
    List<ComponentOwnershipLog> findByProductionOrderId(@Param("orderId") String orderId);

    /**
     * Find all ownership logs for a production order with pagination
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.productionOrder.id = :orderId " +
           "AND col.isDeleted = false")
    Page<ComponentOwnershipLog> findByProductionOrderId(@Param("orderId") String orderId, Pageable pageable);

    /**
     * Find all handoffs where a vendor was the source (sent items)
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.fromVendor.id = :vendorId " +
           "AND col.isDeleted = false " +
           "ORDER BY col.initiatedAt DESC")
    List<ComponentOwnershipLog> findByFromVendorId(@Param("vendorId") String vendorId);

    /**
     * Find all handoffs where a vendor was the destination (received items)
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.toVendor.id = :vendorId " +
           "AND col.isDeleted = false " +
           "ORDER BY col.initiatedAt DESC")
    List<ComponentOwnershipLog> findByToVendorId(@Param("vendorId") String vendorId);

    /**
     * Find all handoffs with a specific status
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.status = :status " +
           "AND col.isDeleted = false " +
           "ORDER BY col.initiatedAt DESC")
    List<ComponentOwnershipLog> findByStatus(@Param("status") HandoffStatus status);

    /**
     * Find all handoffs with a specific status and pagination
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.status = :status " +
           "AND col.isDeleted = false")
    Page<ComponentOwnershipLog> findByStatus(@Param("status") HandoffStatus status, Pageable pageable);

    /**
     * Find pending receipts for a vendor (items they need to confirm receiving)
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.toVendor.id = :vendorId " +
           "AND col.status IN ('INITIATED', 'IN_TRANSIT') " +
           "AND col.isDeleted = false " +
           "ORDER BY col.initiatedAt ASC")
    List<ComponentOwnershipLog> findPendingByToVendorId(@Param("vendorId") String vendorId);

    /**
     * Find pending receipts for a vendor with pagination
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.toVendor.id = :vendorId " +
           "AND col.status IN ('INITIATED', 'IN_TRANSIT') " +
           "AND col.isDeleted = false")
    Page<ComponentOwnershipLog> findPendingByToVendorId(@Param("vendorId") String vendorId, Pageable pageable);

    /**
     * Find the most recent handoff for a production order
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.productionOrder.id = :orderId " +
           "AND col.isDeleted = false " +
           "ORDER BY col.initiatedAt DESC " +
           "LIMIT 1")
    Optional<ComponentOwnershipLog> findLatestByOrderId(@Param("orderId") String orderId);

    /**
     * Find the most recent completed handoff for a production order
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.productionOrder.id = :orderId " +
           "AND col.status = 'RECEIVED' " +
           "AND col.isDeleted = false " +
           "ORDER BY col.receivedAt DESC " +
           "LIMIT 1")
    Optional<ComponentOwnershipLog> findLatestReceivedByOrderId(@Param("orderId") String orderId);

    /**
     * Find all handoffs for a specific stage
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.stage.id = :stageId " +
           "AND col.isDeleted = false " +
           "ORDER BY col.initiatedAt DESC")
    List<ComponentOwnershipLog> findByStageId(@Param("stageId") String stageId);

    /**
     * Find handoffs by type
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.handoffType = :handoffType " +
           "AND col.isDeleted = false " +
           "ORDER BY col.initiatedAt DESC")
    List<ComponentOwnershipLog> findByHandoffType(@Param("handoffType") HandoffType handoffType);

    /**
     * Find handoffs initiated within a date range
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.initiatedAt BETWEEN :startDate AND :endDate " +
           "AND col.isDeleted = false " +
           "ORDER BY col.initiatedAt DESC")
    List<ComponentOwnershipLog> findByInitiatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find overdue handoffs (pending past expected arrival date)
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.status IN ('INITIATED', 'IN_TRANSIT') " +
           "AND col.expectedArrivalDate < CURRENT_DATE " +
           "AND col.isDeleted = false " +
           "ORDER BY col.expectedArrivalDate ASC")
    List<ComponentOwnershipLog> findOverdueHandoffs();

    /**
     * Find overdue handoffs for a specific vendor
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.toVendor.id = :vendorId " +
           "AND col.status IN ('INITIATED', 'IN_TRANSIT') " +
           "AND col.expectedArrivalDate < CURRENT_DATE " +
           "AND col.isDeleted = false " +
           "ORDER BY col.expectedArrivalDate ASC")
    List<ComponentOwnershipLog> findOverdueByToVendorId(@Param("vendorId") String vendorId);

    /**
     * Count pending handoffs for a vendor
     */
    @Query("SELECT COUNT(col) FROM ComponentOwnershipLog col " +
           "WHERE col.toVendor.id = :vendorId " +
           "AND col.status IN ('INITIATED', 'IN_TRANSIT') " +
           "AND col.isDeleted = false")
    long countPendingByToVendorId(@Param("vendorId") String vendorId);

    /**
     * Count handoffs by status for a production order
     */
    @Query("SELECT col.status, COUNT(col) FROM ComponentOwnershipLog col " +
           "WHERE col.productionOrder.id = :orderId " +
           "AND col.isDeleted = false " +
           "GROUP BY col.status")
    List<Object[]> countByStatusForOrder(@Param("orderId") String orderId);

    /**
     * Check if there's any active (non-terminal) handoff for a production order
     */
    @Query("SELECT CASE WHEN COUNT(col) > 0 THEN true ELSE false END " +
           "FROM ComponentOwnershipLog col " +
           "WHERE col.productionOrder.id = :orderId " +
           "AND col.status IN ('INITIATED', 'IN_TRANSIT') " +
           "AND col.isDeleted = false")
    boolean hasActiveHandoff(@Param("orderId") String orderId);

    /**
     * Find all items currently held by a vendor (received but not yet sent out)
     */
    @Query("SELECT col FROM ComponentOwnershipLog col " +
           "WHERE col.toVendor.id = :vendorId " +
           "AND col.status = 'RECEIVED' " +
           "AND col.isDeleted = false " +
           "AND NOT EXISTS (" +
           "    SELECT 1 FROM ComponentOwnershipLog col2 " +
           "    WHERE col2.productionOrder.id = col.productionOrder.id " +
           "    AND col2.fromVendor.id = :vendorId " +
           "    AND col2.initiatedAt > col.receivedAt " +
           "    AND col2.isDeleted = false" +
           ") " +
           "ORDER BY col.receivedAt DESC")
    List<ComponentOwnershipLog> findCurrentlyHeldByVendor(@Param("vendorId") String vendorId);
}
