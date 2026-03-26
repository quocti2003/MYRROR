package com.mirror.product.repository;

import com.mirror.product.entity.CollectionPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionPlanRepository extends JpaRepository<CollectionPlan, UUID> {

    Optional<CollectionPlan> findByName(String name);

    List<CollectionPlan> findByStatus(String status);

    List<CollectionPlan> findByStatusOrderByDeadlineAsc(String status);

    @Query("SELECT cp FROM CollectionPlan cp WHERE cp.status <> 'CANCELLED' " +
            "AND (:status IS NULL OR cp.status = :status)")
    Page<CollectionPlan> findAllWithFilters(
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT cp FROM CollectionPlan cp WHERE cp.status <> 'CANCELLED' " +
            "AND (LOWER(cp.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR cp.status = :status)")
    Page<CollectionPlan> searchByName(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT cp FROM CollectionPlan cp LEFT JOIN FETCH cp.items WHERE cp.id = :id")
    Optional<CollectionPlan> findByIdWithItems(@Param("id") UUID id);

    @Query("SELECT COUNT(cp) FROM CollectionPlan cp WHERE cp.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(cp) FROM CollectionPlan cp WHERE cp.status <> 'CANCELLED'")
    long countActive();
}
