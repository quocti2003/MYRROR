package com.mirror.product.repository.label;

import com.mirror.product.entity.label.LabelTemplate;
import com.mirror.product.enums.LabelTemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RFIDLabelTemplateRepository extends JpaRepository<LabelTemplate, String> {

    Optional<LabelTemplate> findByIdAndIsDeletedFalse(String id);

    List<LabelTemplate> findByStatusAndIsDeletedFalse(LabelTemplateStatus status);

    Optional<LabelTemplate> findByIsDefaultTrueAndIsDeletedFalse();

    Page<LabelTemplate> findByIsDeletedFalse(Pageable pageable);

    Page<LabelTemplate> findByStatusAndIsDeletedFalse(LabelTemplateStatus status, Pageable pageable);

    @Query("SELECT t FROM RFIDLabelTemplate t WHERE t.isDeleted = false " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(CAST(t.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<LabelTemplate> findWithFilters(
        @Param("status") LabelTemplateStatus status,
        @Param("search") String search,
        Pageable pageable
    );

    boolean existsByNameAndIsDeletedFalse(String name);

    @Query("SELECT COUNT(t) FROM RFIDLabelTemplate t WHERE t.status = :status AND t.isDeleted = false")
    long countByStatus(@Param("status") LabelTemplateStatus status);
}
