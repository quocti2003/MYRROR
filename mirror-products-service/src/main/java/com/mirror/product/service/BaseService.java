package com.mirror.product.service;

import com.mirror.product.entity.BaseEntity;
import com.mirror.product.repository.BaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class BaseService<T extends BaseEntity, ID> {
    
    protected final BaseRepository<T, ID> baseRepository;
    
    /**
     * Get all active entities
     */
    @Transactional(readOnly = true)
    public List<T> findAllActive() {
        return baseRepository.findAllActive();
    }
    
    /**
     * Find active entity by ID
     */
    @Transactional(readOnly = true)
    public Optional<T> findActiveById(ID id) {
        return baseRepository.findActiveById(id);
    }
    
    /**
     * Check if active entity exists by ID
     */
    @Transactional(readOnly = true)
    public boolean existsActiveById(ID id) {
        return baseRepository.existsActiveById(id);
    }
    
    /**
     * Save entity
     */
    @Transactional
    public T save(T entity) {
        return baseRepository.save(entity);
    }
    
    /**
     * Save all entities
     */
    @Transactional
    public List<T> saveAll(List<T> entities) {
        return baseRepository.saveAll(entities);
    }
    
    /**
     * Soft delete entity by ID
     */
    @Transactional
    public void softDeleteById(ID id) {
        validateBeforeDelete(id);
        T entity = findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found with id: " + id));
        entity.setIsDeleted(true);
        baseRepository.save(entity);
    }
    
    /**
     * Deactivate entity by ID
     */
    @Transactional
    public void deactivateById(ID id) {
        T entity = findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found with id: " + id));
        entity.setIsActive(false);
        baseRepository.save(entity);
    }
    
    /**
     * Hard delete entity by ID
     */
    @Transactional
    public void deleteById(ID id) {
        if (!baseRepository.existsById(id)) {
            throw new RuntimeException("Entity not found with id: " + id);
        }
        baseRepository.deleteById(id);
    }
    
    /**
     * Get all deleted entities
     */
    @Transactional(readOnly = true)
    public List<T> findAllDeleted() {
        return baseRepository.findAllDeleted();
    }
    
    /**
     * Get all inactive entities
     */
    @Transactional(readOnly = true)
    public List<T> findAllInactive() {
        return baseRepository.findAllInactive();
    }
    
    /**
     * Count active entities
     */
    @Transactional(readOnly = true)
    public long countActive() {
        return baseRepository.countActive();
    }
    
    /**
     * Update entity - to be implemented by child classes
     */
    @Transactional
    public abstract T update(ID id, T entity);
    
    /**
     * Validate entity before save - can be overridden by child classes
     */
    protected void validateBeforeSave(T entity) {
        // Default implementation - can be overridden
    }
    
    /**
     * Validate entity before update - can be overridden by child classes
     */
    protected void validateBeforeUpdate(ID id, T entity) {
        // Default implementation - can be overridden
    }
    
    /**
     * Validate entity before delete - can be overridden by child classes
     */
    protected void validateBeforeDelete(ID id) {
        // Default implementation - can be overridden
    }
}