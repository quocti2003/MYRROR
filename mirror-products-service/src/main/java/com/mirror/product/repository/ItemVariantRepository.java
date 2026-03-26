package com.mirror.product.repository;

import com.mirror.product.entity.ItemVariant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemVariantRepository extends BaseRepository<ItemVariant, String> {
    
    @Override
    @Query("SELECT iv FROM ItemVariant iv WHERE iv.id = :id AND iv.isActive = true AND iv.isDeleted = false")
    Optional<ItemVariant> findActiveById(@Param("id") String id);
    
    @Override
    @Query("SELECT COUNT(iv) > 0 FROM ItemVariant iv WHERE iv.id = :id AND iv.isActive = true AND iv.isDeleted = false")
    boolean existsActiveById(@Param("id") String id);
    
    @Query("SELECT iv FROM ItemVariant iv WHERE iv.itemVariantUrl = :itemVariantUrl AND iv.isActive = true AND iv.isDeleted = false")
    Optional<ItemVariant> findActiveByItemVariantUrl(@Param("itemVariantUrl") String itemVariantUrl);
    
    @Query("SELECT COUNT(iv) > 0 FROM ItemVariant iv WHERE iv.itemVariantUrl = :itemVariantUrl AND iv.isActive = true AND iv.isDeleted = false")
    boolean existsActiveByItemVariantUrl(@Param("itemVariantUrl") String itemVariantUrl);
    
    @Query("SELECT COUNT(iv) > 0 FROM ItemVariant iv WHERE iv.itemVariantUrl = :itemVariantUrl AND iv.id != :itemVariantId AND iv.isActive = true AND iv.isDeleted = false")
    boolean existsActiveByItemVariantUrlAndNotId(@Param("itemVariantUrl") String itemVariantUrl, @Param("itemVariantId") String itemVariantId);
}