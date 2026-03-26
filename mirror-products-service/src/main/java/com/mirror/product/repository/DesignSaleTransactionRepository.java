package com.mirror.product.repository;

import com.mirror.product.entity.DesignSaleTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DesignSaleTransactionRepository extends BaseRepository<DesignSaleTransaction, String> {

    @Override
    @Query("SELECT dst FROM DesignSaleTransaction dst WHERE dst.id = :id AND dst.isActive = true AND dst.isDeleted = false")
    Optional<DesignSaleTransaction> findActiveById(@Param("id") String id);

    @Override
    @Query("SELECT COUNT(dst) > 0 FROM DesignSaleTransaction dst WHERE dst.id = :id AND dst.isActive = true AND dst.isDeleted = false")
    boolean existsActiveById(@Param("id") String id);

    @Query("SELECT dst FROM DesignSaleTransaction dst WHERE dst.designProduct.id = :designProductId AND dst.isActive = true AND dst.isDeleted = false ORDER BY dst.saleDate DESC")
    List<DesignSaleTransaction> findActiveByDesignProductId(@Param("designProductId") String designProductId);

    @Query("SELECT dst FROM DesignSaleTransaction dst WHERE dst.designProduct.designer.id = :designerId AND dst.isActive = true AND dst.isDeleted = false ORDER BY dst.saleDate DESC")
    List<DesignSaleTransaction> findActiveByDesignerId(@Param("designerId") String designerId);

    @Query("SELECT dst FROM DesignSaleTransaction dst WHERE dst.designProduct.designer.id = :designerId AND dst.isActive = true AND dst.isDeleted = false ORDER BY dst.saleDate DESC")
    Page<DesignSaleTransaction> findActiveByDesignerIdPageable(@Param("designerId") String designerId, Pageable pageable);

    @Query("SELECT dst FROM DesignSaleTransaction dst WHERE dst.orderId = :orderId AND dst.isActive = true AND dst.isDeleted = false")
    List<DesignSaleTransaction> findActiveByOrderId(@Param("orderId") String orderId);

    @Query("SELECT dst FROM DesignSaleTransaction dst WHERE dst.processed = :processed AND dst.isActive = true AND dst.isDeleted = false ORDER BY dst.saleDate DESC")
    List<DesignSaleTransaction> findActiveByProcessed(@Param("processed") Boolean processed);

    @Query("SELECT dst FROM DesignSaleTransaction dst WHERE dst.designProduct.designer.id = :designerId AND dst.saleDate BETWEEN :startDate AND :endDate AND dst.isActive = true AND dst.isDeleted = false ORDER BY dst.saleDate DESC")
    List<DesignSaleTransaction> findActiveByDesignerIdAndDateRange(@Param("designerId") String designerId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Analytics queries
    @Query("SELECT SUM(dst.saleAmount) FROM DesignSaleTransaction dst WHERE dst.designProduct.designer.id = :designerId AND dst.isActive = true AND dst.isDeleted = false")
    BigDecimal getTotalSalesAmountByDesignerId(@Param("designerId") String designerId);

    @Query("SELECT SUM(dst.commissionAmount + dst.loyaltyAmount) FROM DesignSaleTransaction dst WHERE dst.designProduct.designer.id = :designerId AND dst.isActive = true AND dst.isDeleted = false")
    BigDecimal getTotalEarningsByDesignerId(@Param("designerId") String designerId);

    @Query("SELECT COUNT(dst) FROM DesignSaleTransaction dst WHERE dst.designProduct.designer.id = :designerId AND dst.isActive = true AND dst.isDeleted = false")
    Long getTotalSalesCountByDesignerId(@Param("designerId") String designerId);

    @Query("SELECT SUM(dst.saleAmount) FROM DesignSaleTransaction dst WHERE dst.designProduct.id = :designProductId AND dst.isActive = true AND dst.isDeleted = false")
    BigDecimal getTotalSalesAmountByDesignProductId(@Param("designProductId") String designProductId);

    @Query("SELECT SUM(dst.commissionAmount + dst.loyaltyAmount) FROM DesignSaleTransaction dst WHERE dst.designProduct.id = :designProductId AND dst.isActive = true AND dst.isDeleted = false")
    BigDecimal getTotalEarningsByDesignProductId(@Param("designProductId") String designProductId);

    @Query("SELECT COUNT(dst) FROM DesignSaleTransaction dst WHERE dst.designProduct.id = :designProductId AND dst.isActive = true AND dst.isDeleted = false")
    Long getTotalSalesCountByDesignProductId(@Param("designProductId") String designProductId);

    // Monthly analytics
    @Query("SELECT SUM(dst.saleAmount) FROM DesignSaleTransaction dst WHERE dst.designProduct.designer.id = :designerId AND dst.saleDate BETWEEN :startDate AND :endDate AND dst.isActive = true AND dst.isDeleted = false")
    BigDecimal getMonthlySalesAmountByDesignerId(@Param("designerId") String designerId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(dst.commissionAmount + dst.loyaltyAmount) FROM DesignSaleTransaction dst WHERE dst.designProduct.designer.id = :designerId AND dst.saleDate BETWEEN :startDate AND :endDate AND dst.isActive = true AND dst.isDeleted = false")
    BigDecimal getMonthlyEarningsByDesignerId(@Param("designerId") String designerId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Rating analytics
    @Query("SELECT AVG(dst.customerRating) FROM DesignSaleTransaction dst WHERE dst.designProduct.designer.id = :designerId AND dst.customerRating IS NOT NULL AND dst.isActive = true AND dst.isDeleted = false")
    BigDecimal getAverageRatingByDesignerId(@Param("designerId") String designerId);

    @Query("SELECT COUNT(dst) FROM DesignSaleTransaction dst WHERE dst.designProduct.designer.id = :designerId AND dst.customerRating IS NOT NULL AND dst.isActive = true AND dst.isDeleted = false")
    Long getRatingCountByDesignerId(@Param("designerId") String designerId);
}