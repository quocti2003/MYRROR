package com.mirror.product.repository;

import com.mirror.product.entity.WorkflowStage;
import com.mirror.product.enums.PartnerCapabilityType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStageRepository extends BaseRepository<WorkflowStage, String> {

    @Override
    @Query("SELECT ws FROM WorkflowStage ws WHERE ws.id = :id AND ws.isActive = true AND ws.isDeleted = false")
    Optional<WorkflowStage> findActiveById(@Param("id") String id);

    @Query("SELECT ws FROM WorkflowStage ws WHERE ws.template.id = :templateId AND ws.isActive = true AND ws.isDeleted = false ORDER BY ws.stageOrder")
    List<WorkflowStage> findActiveByTemplateIdOrderByStageOrder(@Param("templateId") String templateId);

    @Query("SELECT ws FROM WorkflowStage ws WHERE ws.template.id = :templateId AND ws.stageOrder = :stageOrder AND ws.isActive = true AND ws.isDeleted = false")
    Optional<WorkflowStage> findActiveByTemplateIdAndStageOrder(@Param("templateId") String templateId, @Param("stageOrder") Integer stageOrder);

    @Query("SELECT ws FROM WorkflowStage ws WHERE ws.template.id = :templateId AND ws.name = :name AND ws.isActive = true AND ws.isDeleted = false")
    Optional<WorkflowStage> findActiveByTemplateIdAndName(@Param("templateId") String templateId, @Param("name") String name);

    @Query("SELECT ws FROM WorkflowStage ws WHERE ws.requiredCapability = :capability AND ws.isActive = true AND ws.isDeleted = false")
    List<WorkflowStage> findActiveByRequiredCapability(@Param("capability") PartnerCapabilityType capability);

    @Query("SELECT ws FROM WorkflowStage ws WHERE ws.template.id = :templateId AND ws.isFinalStage = true AND ws.isActive = true AND ws.isDeleted = false")
    Optional<WorkflowStage> findFinalStageByTemplateId(@Param("templateId") String templateId);

    @Query("SELECT MAX(ws.stageOrder) FROM WorkflowStage ws WHERE ws.template.id = :templateId AND ws.isActive = true AND ws.isDeleted = false")
    Integer findMaxStageOrderByTemplateId(@Param("templateId") String templateId);

    @Query("SELECT COUNT(ws) FROM WorkflowStage ws WHERE ws.template.id = :templateId AND ws.isActive = true AND ws.isDeleted = false")
    int countByTemplateId(@Param("templateId") String templateId);

    @Query("SELECT ws FROM WorkflowStage ws WHERE ws.template.id = :templateId AND ws.stageOrder > :stageOrder AND ws.isActive = true AND ws.isDeleted = false ORDER BY ws.stageOrder")
    List<WorkflowStage> findStagesAfter(@Param("templateId") String templateId, @Param("stageOrder") Integer stageOrder);

    @Query("SELECT ws FROM WorkflowStage ws WHERE ws.template.id = :templateId AND ws.stageOrder < :stageOrder AND ws.isActive = true AND ws.isDeleted = false ORDER BY ws.stageOrder DESC")
    List<WorkflowStage> findStagesBefore(@Param("templateId") String templateId, @Param("stageOrder") Integer stageOrder);
}
