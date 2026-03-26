package com.mirror.product.repository;

import com.mirror.product.entity.Component;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComponentRepository extends BaseRepository<Component, String> {

    @Override
    @Query("SELECT c FROM Component c WHERE c.id = :id AND c.isActive = true AND c.isDeleted = false")
    Optional<Component> findActiveById(@Param("id") String id);

    @Override
    @Query("SELECT COUNT(c) > 0 FROM Component c WHERE c.id = :id AND c.isActive = true AND c.isDeleted = false")
    boolean existsActiveById(@Param("id") String id);

    @Query("SELECT c FROM Component c WHERE c.product.id = :productId AND c.isActive = true AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Component> findActiveByProductId(@Param("productId") String productId);

    @Query("SELECT c FROM Component c WHERE c.componentName = :componentName AND c.isActive = true AND c.isDeleted = false")
    Optional<Component> findActiveByComponentName(@Param("componentName") String componentName);

    @Query("SELECT COUNT(c) > 0 FROM Component c WHERE c.componentName = :componentName AND c.product.id = :productId AND c.isActive = true AND c.isDeleted = false")
    boolean existsActiveByComponentNameAndProductId(@Param("componentName") String componentName, @Param("productId") String productId);

    @Query("SELECT COUNT(c) > 0 FROM Component c WHERE c.componentName = :componentName AND c.product.id = :productId AND c.id != :componentId AND c.isActive = true AND c.isDeleted = false")
    boolean existsActiveByComponentNameAndProductIdAndNotId(@Param("componentName") String componentName, @Param("productId") String productId, @Param("componentId") String componentId);

    @Query("SELECT COUNT(c) FROM Component c WHERE c.product.id = :productId AND c.isActive = true AND c.isDeleted = false")
    long countActiveByProductId(@Param("productId") String productId);
}
