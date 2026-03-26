package com.mirror.product.repository;

import com.mirror.product.entity.ProductionPlan;
import com.mirror.product.enums.ProductionPlanStatus;
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
public interface ProductionPlanRepository extends BaseRepository<ProductionPlan, String> {

    @Override
    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.id = :id AND pp.isActive = true AND pp.isDeleted = false")
    Optional<ProductionPlan> findActiveById(@Param("id") String id);

    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.name = :name AND pp.isActive = true AND pp.isDeleted = false")
    Optional<ProductionPlan> findActiveByName(@Param("name") String name);

    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.collectionPlanId = :collectionPlanId AND pp.isActive = true AND pp.isDeleted = false")
    List<ProductionPlan> findActiveByCollectionPlanId(@Param("collectionPlanId") UUID collectionPlanId);

    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.workflowTemplate.id = :templateId AND pp.isActive = true AND pp.isDeleted = false")
    List<ProductionPlan> findActiveByWorkflowTemplateId(@Param("templateId") String templateId);

    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.status = :status AND pp.isActive = true AND pp.isDeleted = false")
    List<ProductionPlan> findActiveByStatus(@Param("status") ProductionPlanStatus status);

    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.isActive = true AND pp.isDeleted = false AND " +
           "(LOWER(pp.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pp.notes) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ProductionPlan> searchActive(@Param("search") String search, Pageable pageable);

    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.isActive = true AND pp.isDeleted = false " +
           "AND (:status IS NULL OR pp.status = :status) " +
           "AND (:collectionPlanId IS NULL OR pp.collectionPlanId = :collectionPlanId)")
    Page<ProductionPlan> findAllActiveWithFilters(
        @Param("status") ProductionPlanStatus status,
        @Param("collectionPlanId") UUID collectionPlanId,
        Pageable pageable
    );

    @Query("SELECT pp FROM ProductionPlan pp " +
           "LEFT JOIN FETCH pp.workflowTemplate wt " +
           "WHERE pp.id = :id AND pp.isActive = true AND pp.isDeleted = false")
    Optional<ProductionPlan> findActiveByIdWithWorkflowTemplate(@Param("id") String id);

    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.targetStartDate BETWEEN :startDate AND :endDate AND pp.isActive = true AND pp.isDeleted = false")
    List<ProductionPlan> findActiveByTargetStartDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.status IN :statuses AND pp.isActive = true AND pp.isDeleted = false ORDER BY pp.targetStartDate")
    List<ProductionPlan> findActiveByStatusIn(@Param("statuses") List<ProductionPlanStatus> statuses);

    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.status IN ('APPROVED', 'IN_PRODUCTION') AND pp.isActive = true AND pp.isDeleted = false ORDER BY pp.targetStartDate")
    List<ProductionPlan> findAllActivePlans();

    @Query("SELECT COUNT(pp) > 0 FROM ProductionPlan pp WHERE pp.collectionPlanId = :collectionPlanId AND pp.isActive = true AND pp.isDeleted = false")
    boolean existsByCollectionPlanId(@Param("collectionPlanId") UUID collectionPlanId);

    @Query("SELECT COUNT(pp) FROM ProductionPlan pp WHERE pp.workflowTemplate.id = :templateId AND pp.isActive = true AND pp.isDeleted = false")
    int countByWorkflowTemplateId(@Param("templateId") String templateId);

    @Query("SELECT COUNT(pp) FROM ProductionPlan pp WHERE pp.workflowTemplate.id = :templateId AND pp.isDeleted = false")
    long countByWorkflowTemplateIdAndIsDeletedFalse(@Param("templateId") String templateId);

    @Query("SELECT EXISTS(SELECT 1 FROM ProductionPlan pp WHERE pp.workflowTemplate.id = :templateId AND pp.isDeleted = false)")
    boolean existsByWorkflowTemplateIdAndIsDeletedFalse(@Param("templateId") String templateId);
}
