package com.mirror.product.repository;

import com.mirror.product.entity.JewelryTechnicalReport;
import com.mirror.product.enums.JTRCStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JewelryTechnicalReportRepository extends BaseRepository<JewelryTechnicalReport, String> {

    @Override
    @Query("SELECT j FROM JewelryTechnicalReport j WHERE j.id = :id AND j.isActive = true AND j.isDeleted = false")
    Optional<JewelryTechnicalReport> findActiveById(@Param("id") String id);

    @Query("SELECT j FROM JewelryTechnicalReport j WHERE j.reportNumber = :reportNumber AND j.isActive = true AND j.isDeleted = false")
    Optional<JewelryTechnicalReport> findActiveByReportNumber(@Param("reportNumber") String reportNumber);

    @Query("SELECT COUNT(j) > 0 FROM JewelryTechnicalReport j WHERE j.reportNumber = :reportNumber AND j.isActive = true AND j.isDeleted = false")
    boolean existsActiveByReportNumber(@Param("reportNumber") String reportNumber);

    @Query("SELECT j FROM JewelryTechnicalReport j WHERE j.collection = :collection AND j.isActive = true AND j.isDeleted = false")
    List<JewelryTechnicalReport> findActiveByCollection(@Param("collection") String collection);

    @Query("SELECT j FROM JewelryTechnicalReport j WHERE j.status = :status AND j.isActive = true AND j.isDeleted = false")
    List<JewelryTechnicalReport> findActiveByStatus(@Param("status") JTRCStatus status);

    @Query("SELECT j FROM JewelryTechnicalReport j WHERE j.category = :category AND j.isActive = true AND j.isDeleted = false")
    List<JewelryTechnicalReport> findActiveByCategory(@Param("category") String category);

    @Query("SELECT j FROM JewelryTechnicalReport j WHERE j.collectionPlanItemId = :itemId AND j.isActive = true AND j.isDeleted = false")
    Optional<JewelryTechnicalReport> findActiveByCollectionPlanItemId(@Param("itemId") String itemId);

    @Query("SELECT j FROM JewelryTechnicalReport j WHERE j.productId = :productId AND j.isActive = true AND j.isDeleted = false")
    Optional<JewelryTechnicalReport> findActiveByProductId(@Param("productId") String productId);

    @Query("SELECT j FROM JewelryTechnicalReport j WHERE j.isActive = true AND j.isDeleted = false AND " +
           "(LOWER(j.reportNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(j.collection) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(j.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(j.projectId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<JewelryTechnicalReport> searchActive(@Param("search") String search, Pageable pageable);

    @Query("SELECT j FROM JewelryTechnicalReport j WHERE j.isActive = true AND j.isDeleted = false " +
           "AND (:collection IS NULL OR j.collection = :collection) " +
           "AND (:status IS NULL OR j.status = :status) " +
           "AND (:category IS NULL OR j.category = :category)")
    Page<JewelryTechnicalReport> findAllActiveWithFilters(
        @Param("collection") String collection,
        @Param("status") JTRCStatus status,
        @Param("category") String category,
        Pageable pageable
    );

    @Query("SELECT j FROM JewelryTechnicalReport j " +
           "LEFT JOIN FETCH j.metalComponent " +
           "LEFT JOIN FETCH j.stoneComponents " +
           "LEFT JOIN FETCH j.laborComponents " +
           "WHERE j.id = :id AND j.isActive = true AND j.isDeleted = false")
    Optional<JewelryTechnicalReport> findActiveByIdWithComponents(@Param("id") String id);

    @Query("SELECT DISTINCT j.collection FROM JewelryTechnicalReport j WHERE j.isActive = true AND j.isDeleted = false ORDER BY j.collection")
    List<String> findDistinctCollections();

    @Query("SELECT DISTINCT j.season FROM JewelryTechnicalReport j WHERE j.isActive = true AND j.isDeleted = false ORDER BY j.season")
    List<String> findDistinctSeasons();

    @Query("SELECT DISTINCT j.category FROM JewelryTechnicalReport j WHERE j.isActive = true AND j.isDeleted = false ORDER BY j.category")
    List<String> findDistinctCategories();
}
