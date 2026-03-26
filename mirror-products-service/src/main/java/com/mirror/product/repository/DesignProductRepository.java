package com.mirror.product.repository;

import com.mirror.product.entity.DesignProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DesignProductRepository extends BaseRepository<DesignProduct, String> {

    @Override
    @Query("SELECT dp FROM DesignProduct dp WHERE dp.id = :id AND dp.isActive = true AND dp.isDeleted = false")
    Optional<DesignProduct> findActiveById(@Param("id") String id);

    @Override
    @Query("SELECT COUNT(dp) > 0 FROM DesignProduct dp WHERE dp.id = :id AND dp.isActive = true AND dp.isDeleted = false")
    boolean existsActiveById(@Param("id") String id);

    @Query("SELECT dp FROM DesignProduct dp WHERE dp.designer.id = :designerId AND dp.isActive = true AND dp.isDeleted = false")
    List<DesignProduct> findActiveByDesignerId(@Param("designerId") String designerId);

    @Query("SELECT dp FROM DesignProduct dp WHERE dp.product.id = :productId AND dp.isActive = true AND dp.isDeleted = false")
    Optional<DesignProduct> findActiveByProductId(@Param("productId") String productId);

    @Query("SELECT dp FROM DesignProduct dp WHERE dp.designer.id = :designerId AND dp.designStatus = :status AND dp.isActive = true AND dp.isDeleted = false")
    List<DesignProduct> findActiveByDesignerIdAndStatus(@Param("designerId") String designerId, @Param("status") String status);

    @Query("SELECT dp FROM DesignProduct dp WHERE dp.featuredDesign = true AND dp.isActive = true AND dp.isDeleted = false")
    List<DesignProduct> findActiveFeaturedDesigns();

    @Query("SELECT dp FROM DesignProduct dp WHERE dp.designer.id = :designerId AND dp.isActive = true AND dp.isDeleted = false ORDER BY dp.totalSalesAmount DESC")
    Page<DesignProduct> findTopDesignsByDesignerId(@Param("designerId") String designerId, Pageable pageable);

    @Query("SELECT dp FROM DesignProduct dp WHERE dp.designer.id = :designerId AND dp.isActive = true AND dp.isDeleted = false ORDER BY dp.totalSalesCount DESC")
    List<DesignProduct> findBestSellingDesignsByDesignerId(@Param("designerId") String designerId);

    @Query("SELECT COUNT(dp) FROM DesignProduct dp WHERE dp.designer.id = :designerId AND dp.isActive = true AND dp.isDeleted = false")
    Long countActiveDesignsByDesignerId(@Param("designerId") String designerId);

    @Query("SELECT SUM(dp.totalSalesAmount) FROM DesignProduct dp WHERE dp.designer.id = :designerId AND dp.isActive = true AND dp.isDeleted = false")
    BigDecimal getTotalSalesAmountByDesignerId(@Param("designerId") String designerId);

    @Query("SELECT SUM(dp.totalCommissionEarned + dp.totalLoyaltyEarned) FROM DesignProduct dp WHERE dp.designer.id = :designerId AND dp.isActive = true AND dp.isDeleted = false")
    BigDecimal getTotalEarningsByDesignerId(@Param("designerId") String designerId);

    @Query("SELECT SUM(dp.totalSalesCount) FROM DesignProduct dp WHERE dp.designer.id = :designerId AND dp.isActive = true AND dp.isDeleted = false")
    Long getTotalSalesCountByDesignerId(@Param("designerId") String designerId);

    @Query("SELECT AVG(dp.averageRating) FROM DesignProduct dp WHERE dp.designer.id = :designerId AND dp.averageRating IS NOT NULL AND dp.isActive = true AND dp.isDeleted = false")
    BigDecimal getAverageRatingByDesignerId(@Param("designerId") String designerId);
}