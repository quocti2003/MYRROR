package com.mirror.product.repository;

import com.mirror.product.entity.WorkflowTemplate;
import com.mirror.product.enums.WorkflowTemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTemplateRepository extends BaseRepository<WorkflowTemplate, String> {

    @Override
    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.id = :id AND wt.isActive = true AND wt.isDeleted = false")
    Optional<WorkflowTemplate> findActiveById(@Param("id") String id);

    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.name = :name AND wt.isActive = true AND wt.isDeleted = false")
    Optional<WorkflowTemplate> findActiveByName(@Param("name") String name);

    @Query("SELECT COUNT(wt) > 0 FROM WorkflowTemplate wt WHERE wt.name = :name AND wt.isActive = true AND wt.isDeleted = false")
    boolean existsActiveByName(@Param("name") String name);

    @Query("SELECT COUNT(wt) > 0 FROM WorkflowTemplate wt WHERE wt.name = :name AND wt.id <> :id AND wt.isActive = true AND wt.isDeleted = false")
    boolean existsActiveByNameAndIdNot(@Param("name") String name, @Param("id") String id);

    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.category = :category AND wt.isActive = true AND wt.isDeleted = false")
    List<WorkflowTemplate> findActiveByCategory(@Param("category") String category);

    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.category = :category AND wt.isDefault = true AND wt.status = 'ACTIVE' AND wt.isActive = true AND wt.isDeleted = false")
    Optional<WorkflowTemplate> findDefaultByCategory(@Param("category") String category);

    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.status = :status AND wt.isActive = true AND wt.isDeleted = false")
    List<WorkflowTemplate> findActiveByStatus(@Param("status") WorkflowTemplateStatus status);

    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.isActive = true AND wt.isDeleted = false AND " +
           "(LOWER(wt.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(wt.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(wt.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<WorkflowTemplate> searchActive(@Param("search") String search, Pageable pageable);

    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.isActive = true AND wt.isDeleted = false " +
           "AND (:category IS NULL OR wt.category = :category) " +
           "AND (:status IS NULL OR wt.status = :status)")
    Page<WorkflowTemplate> findAllActiveWithFilters(
        @Param("category") String category,
        @Param("status") WorkflowTemplateStatus status,
        Pageable pageable
    );

    @Query("SELECT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.stages " +
           "WHERE wt.id = :id AND wt.isActive = true AND wt.isDeleted = false")
    Optional<WorkflowTemplate> findActiveByIdWithStages(@Param("id") String id);

    @Query("SELECT DISTINCT wt.category FROM WorkflowTemplate wt WHERE wt.isActive = true AND wt.isDeleted = false AND wt.category IS NOT NULL ORDER BY wt.category")
    List<String> findDistinctCategories();

    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.status = 'ACTIVE' AND wt.isActive = true AND wt.isDeleted = false ORDER BY wt.name")
    List<WorkflowTemplate> findAllActiveTemplates();

    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.category = :category AND wt.isDefault = true AND wt.isActive = true AND wt.isDeleted = false AND wt.id <> :excludeId")
    List<WorkflowTemplate> findOtherDefaultsInCategory(@Param("category") String category, @Param("excludeId") String excludeId);
}
