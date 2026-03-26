package com.mirror.product.repository;

import com.mirror.product.entity.CollectionProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionProductRepository extends JpaRepository<CollectionProduct, String> {
    
    @Query("SELECT cp FROM CollectionProduct cp WHERE cp.collectionId = :collectionId ORDER BY cp.sortOrder ASC")
    List<CollectionProduct> findByCollectionIdOrderBySortOrder(@Param("collectionId") String collectionId);
    
    @Query("SELECT cp FROM CollectionProduct cp WHERE cp.productId = :productId")
    List<CollectionProduct> findByProductId(@Param("productId") String productId);
    
    @Query("SELECT cp FROM CollectionProduct cp WHERE cp.collectionId = :collectionId AND cp.productId = :productId")
    Optional<CollectionProduct> findByCollectionIdAndProductId(@Param("collectionId") String collectionId, @Param("productId") String productId);
    
    @Query("SELECT COUNT(cp) FROM CollectionProduct cp WHERE cp.collectionId = :collectionId AND cp.productId = :productId")
    long countByCollectionIdAndProductId(@Param("collectionId") String collectionId, @Param("productId") String productId);
    
    @Query("SELECT cp FROM CollectionProduct cp WHERE cp.collectionId = :collectionId AND cp.isHeroProduct = true")
    List<CollectionProduct> findHeroProductsByCollectionId(@Param("collectionId") String collectionId);
    
    @Query("SELECT COUNT(cp) FROM CollectionProduct cp WHERE cp.collectionId = :collectionId")
    long countByCollectionId(@Param("collectionId") String collectionId);
    
    @Query("SELECT COUNT(cp) FROM CollectionProduct cp WHERE cp.productId = :productId")
    long countByProductId(@Param("productId") String productId);
    
    boolean existsByCollectionIdAndProductId(String collectionId, String productId);
}