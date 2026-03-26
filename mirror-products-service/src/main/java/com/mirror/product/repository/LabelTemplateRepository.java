package com.mirror.product.repository;

import com.mirror.product.entity.LabelTemplate;
import com.mirror.product.enums.LabelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabelTemplateRepository extends BaseRepository<LabelTemplate, String> {

    @Override
    @Query("SELECT lt FROM LabelTemplate lt WHERE lt.id = :id AND lt.isActive = true AND lt.isDeleted = false")
    Optional<LabelTemplate> findActiveById(@Param("id") String id);

    @Query("SELECT lt FROM LabelTemplate lt WHERE lt.name = :name AND lt.isActive = true AND lt.isDeleted = false")
    Optional<LabelTemplate> findActiveByName(@Param("name") String name);

    @Query("SELECT COUNT(lt) > 0 FROM LabelTemplate lt WHERE lt.name = :name AND lt.isActive = true AND lt.isDeleted = false")
    boolean existsActiveByName(@Param("name") String name);

    @Query("SELECT COUNT(lt) > 0 FROM LabelTemplate lt WHERE lt.name = :name AND lt.id <> :id AND lt.isActive = true AND lt.isDeleted = false")
    boolean existsActiveByNameAndIdNot(@Param("name") String name, @Param("id") String id);

    @Query("SELECT lt FROM LabelTemplate lt WHERE lt.labelType = :labelType AND lt.isActive = true AND lt.isDeleted = false ORDER BY lt.name")
    List<LabelTemplate> findActiveByLabelType(@Param("labelType") LabelType labelType);

    @Query("SELECT lt FROM LabelTemplate lt WHERE lt.labelType = :labelType AND lt.isDefault = true AND lt.isActive = true AND lt.isDeleted = false")
    Optional<LabelTemplate> findDefaultByLabelType(@Param("labelType") LabelType labelType);

    @Query("SELECT lt FROM LabelTemplate lt WHERE lt.isActive = true AND lt.isDeleted = false AND " +
           "(LOWER(lt.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(lt.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<LabelTemplate> searchActive(@Param("search") String search, Pageable pageable);

    @Query("SELECT lt FROM LabelTemplate lt WHERE lt.isActive = true AND lt.isDeleted = false " +
           "AND (:labelType IS NULL OR lt.labelType = :labelType)")
    Page<LabelTemplate> findAllActiveWithFilters(
        @Param("labelType") LabelType labelType,
        Pageable pageable
    );

    @Query("SELECT DISTINCT lt.labelType FROM LabelTemplate lt WHERE lt.isActive = true AND lt.isDeleted = false ORDER BY lt.labelType")
    List<LabelType> findDistinctLabelTypes();

    @Query("SELECT lt FROM LabelTemplate lt WHERE lt.labelType = :labelType AND lt.isDefault = true AND lt.isActive = true AND lt.isDeleted = false AND lt.id <> :excludeId")
    List<LabelTemplate> findOtherDefaultsForLabelType(@Param("labelType") LabelType labelType, @Param("excludeId") String excludeId);

    @Query("SELECT lt FROM LabelTemplate lt WHERE lt.isDefault = true AND lt.isActive = true AND lt.isDeleted = false ORDER BY lt.labelType")
    List<LabelTemplate> findAllDefaults();
}
