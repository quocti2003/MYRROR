package com.mirror.product.repository;

import com.mirror.product.entity.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity, ID> extends JpaRepository<T, ID> {
    
    /**
     * Find all active entities (is_active = true and is_deleted = false)
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.isActive = true AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<T> findAllActive();
    
    /**
     * Find active entity by ID - this method will be overridden by concrete repositories
     * since they know their specific ID field name
     */
    default Optional<T> findActiveById(ID id) {
        return findById(id).filter(entity -> entity.getIsActive() && !entity.getIsDeleted());
    }
    
    /**
     * Check if active entity exists by ID - this method will be overridden by concrete repositories
     */
    default boolean existsActiveById(ID id) {
        return findById(id)
                .map(entity -> entity.getIsActive() && !entity.getIsDeleted())
                .orElse(false);
    }
    
    /**
     * Soft delete entity (set is_deleted = true) - will be implemented in service layer
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.isDeleted = true WHERE e = :entity")
    void softDelete(@Param("entity") T entity);
    
    /**
     * Deactivate entity (set is_active = false) - will be implemented in service layer
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.isActive = false WHERE e = :entity")
    void deactivate(@Param("entity") T entity);
    
    /**
     * Find all deleted entities (is_deleted = true)
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.isDeleted = true ORDER BY e.createdAt DESC")
    List<T> findAllDeleted();
    
    /**
     * Find all inactive entities (is_active = false and is_deleted = false)
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.isActive = false AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<T> findAllInactive();
    
    /**
     * Count active entities
     */
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.isActive = true AND e.isDeleted = false")
    long countActive();
}