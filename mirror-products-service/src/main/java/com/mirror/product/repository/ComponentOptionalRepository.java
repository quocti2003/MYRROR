package com.mirror.product.repository;

import com.mirror.product.entity.ComponentOptional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComponentOptionalRepository extends BaseRepository<ComponentOptional, String> {
    
    @Override
    @Query("SELECT co FROM ComponentOptional co WHERE co.id = :id AND co.isActive = true AND co.isDeleted = false")
    Optional<ComponentOptional> findActiveById(@Param("id") String id);
    
    @Override
    @Query("SELECT COUNT(co) > 0 FROM ComponentOptional co WHERE co.id = :id AND co.isActive = true AND co.isDeleted = false")
    boolean existsActiveById(@Param("id") String id);
    
    @Query("SELECT co FROM ComponentOptional co WHERE co.component.id = :componentId AND co.isActive = true AND co.isDeleted = false ORDER BY co.createdAt DESC")
    List<ComponentOptional> findActiveByComponentId(@Param("componentId") String componentId);
    
    @Query("SELECT co FROM ComponentOptional co WHERE co.componentOptionalName = :componentOptionalName AND co.isActive = true AND co.isDeleted = false")
    Optional<ComponentOptional> findActiveByComponentOptionalName(@Param("componentOptionalName") String componentOptionalName);
    
    @Query("SELECT COUNT(co) > 0 FROM ComponentOptional co WHERE co.componentOptionalName = :componentOptionalName AND co.component.id = :componentId AND co.isActive = true AND co.isDeleted = false")
    boolean existsActiveByComponentOptionalNameAndComponentId(@Param("componentOptionalName") String componentOptionalName, @Param("componentId") String componentId);
    
    @Query("SELECT COUNT(co) > 0 FROM ComponentOptional co WHERE co.componentOptionalName = :componentOptionalName AND co.component.id = :componentId AND co.id != :componentOptionalId AND co.isActive = true AND co.isDeleted = false")
    boolean existsActiveByComponentOptionalNameAndComponentIdAndNotId(@Param("componentOptionalName") String componentOptionalName, @Param("componentId") String componentId, @Param("componentOptionalId") String componentOptionalId);
}