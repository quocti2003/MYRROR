package com.mirror.product.repository.misa;

import com.mirror.product.entity.misa.MisaProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MisaProductCategoryRepository extends JpaRepository<MisaProductCategory, Long> {

    Optional<MisaProductCategory> findByCategoryId(String categoryId);

    Optional<MisaProductCategory> findByCategoryCode(String categoryCode);

    List<MisaProductCategory> findByParentId(String parentId);

    List<MisaProductCategory> findByParentIdIsNull();

    List<MisaProductCategory> findByGrade(Integer grade);

    List<MisaProductCategory> findByIsInactiveFalse();

    List<MisaProductCategory> findByIsLeafTrue();

    List<MisaProductCategory> findBySyncStatus(MisaProductCategory.SyncStatus syncStatus);

    @Query("SELECT c FROM MisaProductCategory c WHERE c.categoryName LIKE %:name%")
    List<MisaProductCategory> findByCategoryNameContaining(@Param("name") String name);

    @Query("SELECT c FROM MisaProductCategory c WHERE c.lastSyncDate < :cutoffDate")
    List<MisaProductCategory> findCategoriesNeedingSync(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT c FROM MisaProductCategory c WHERE c.parentId IS NULL ORDER BY c.sortOrder, c.categoryName")
    List<MisaProductCategory> findRootCategoriesOrdered();

    @Query("SELECT c FROM MisaProductCategory c WHERE c.parentId = :parentId ORDER BY c.sortOrder, c.categoryName")
    List<MisaProductCategory> findChildCategoriesOrdered(@Param("parentId") String parentId);

    @Query("SELECT COUNT(c) FROM MisaProductCategory c WHERE c.isInactive = false")
    Long countActiveCategories();

    @Query("SELECT COUNT(c) FROM MisaProductCategory c WHERE c.parentId = :parentId")
    Long countChildCategories(@Param("parentId") String parentId);

    @Query("SELECT DISTINCT c.grade FROM MisaProductCategory c WHERE c.grade IS NOT NULL ORDER BY c.grade")
    List<Integer> findDistinctGrades();

    Page<MisaProductCategory> findByIsInactiveFalse(Pageable pageable);

    Page<MisaProductCategory> findByCategoryNameContaining(String categoryName, Pageable pageable);

    boolean existsByCategoryId(String categoryId);

    boolean existsByCategoryCode(String categoryCode);

    @Query("SELECT c FROM MisaProductCategory c WHERE c.fullPath LIKE %:path%")
    List<MisaProductCategory> findByFullPathContaining(@Param("path") String path);
}
