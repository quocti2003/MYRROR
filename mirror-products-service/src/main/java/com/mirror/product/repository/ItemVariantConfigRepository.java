package com.mirror.product.repository;

import com.mirror.product.entity.ItemVariantConfig;
import com.mirror.product.entity.ItemVariant;
import com.mirror.product.entity.ComponentOptional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemVariantConfigRepository extends BaseRepository<ItemVariantConfig, String> {
    
    @Override
    @Query("SELECT ivc FROM ItemVariantConfig ivc WHERE ivc.id = :id AND ivc.isActive = true AND ivc.isDeleted = false")
    Optional<ItemVariantConfig> findActiveById(@Param("id") String id);
    
    @Override
    @Query("SELECT COUNT(ivc) > 0 FROM ItemVariantConfig ivc WHERE ivc.id = :id AND ivc.isActive = true AND ivc.isDeleted = false")
    boolean existsActiveById(@Param("id") String id);
    
    @Query("SELECT ivc FROM ItemVariantConfig ivc WHERE ivc.itemVariant.id = :itemVariantId AND ivc.isActive = true AND ivc.isDeleted = false ORDER BY ivc.createdAt DESC")
    List<ItemVariantConfig> findActiveByItemVariantId(@Param("itemVariantId") String itemVariantId);
    
    @Query("SELECT ivc FROM ItemVariantConfig ivc WHERE ivc.componentOptional.id = :componentOptionalId AND ivc.isActive = true AND ivc.isDeleted = false ORDER BY ivc.createdAt DESC")
    List<ItemVariantConfig> findActiveByComponentOptionalId(@Param("componentOptionalId") String componentOptionalId);
    
    @Query("SELECT ivc FROM ItemVariantConfig ivc WHERE ivc.itemVariant.id = :itemVariantId AND ivc.componentOptional.id = :componentOptionalId AND ivc.isActive = true AND ivc.isDeleted = false")
    Optional<ItemVariantConfig> findActiveByItemVariantIdAndComponentOptionalId(@Param("itemVariantId") String itemVariantId, @Param("componentOptionalId") String componentOptionalId);
    
    @Query("SELECT COUNT(ivc) > 0 FROM ItemVariantConfig ivc WHERE ivc.itemVariant.id = :itemVariantId AND ivc.componentOptional.id = :componentOptionalId AND ivc.isActive = true AND ivc.isDeleted = false")
    boolean existsActiveByItemVariantIdAndComponentOptionalId(@Param("itemVariantId") String itemVariantId, @Param("componentOptionalId") String componentOptionalId);
    
    @Query("SELECT COUNT(ivc) > 0 FROM ItemVariantConfig ivc WHERE ivc.itemVariant.id = :itemVariantId AND ivc.componentOptional.id = :componentOptionalId AND ivc.id != :id AND ivc.isActive = true AND ivc.isDeleted = false")
    boolean existsActiveByItemVariantIdAndComponentOptionalIdAndNotId(@Param("itemVariantId") String itemVariantId, @Param("componentOptionalId") String componentOptionalId, @Param("id") String id);
}