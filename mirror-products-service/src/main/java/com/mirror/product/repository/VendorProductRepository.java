package com.mirror.product.repository;

import com.mirror.product.entity.VendorProduct;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorProductRepository extends BaseRepository<VendorProduct, String> {
    
    @Override
    @Query("SELECT vp FROM VendorProduct vp WHERE vp.id = :id AND vp.isActive = true AND vp.isDeleted = false")
    Optional<VendorProduct> findActiveById(@Param("id") String id);
    
    @Override
    @Query("SELECT COUNT(vp) > 0 FROM VendorProduct vp WHERE vp.id = :id AND vp.isActive = true AND vp.isDeleted = false")
    boolean existsActiveById(@Param("id") String id);
    
    @Query("SELECT vp FROM VendorProduct vp WHERE vp.vendor.id = :vendorId AND vp.isActive = true AND vp.isDeleted = false")
    List<VendorProduct> findByVendorId(@Param("vendorId") String vendorId);
    
    @Query("SELECT vp FROM VendorProduct vp WHERE vp.product.id = :productId AND vp.isActive = true AND vp.isDeleted = false")
    Optional<VendorProduct> findByProductId(@Param("productId") String productId);
    
    @Query("SELECT COUNT(vp) FROM VendorProduct vp WHERE vp.vendor.id = :vendorId AND vp.isActive = true AND vp.isDeleted = false")
    long countByVendorId(@Param("vendorId") String vendorId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM VendorProduct vp WHERE vp.product.id = :productId")
    void deleteByProductId(@Param("productId") String productId);

    /**
     * Batch load vendor products for multiple product IDs (for N+1 optimization)
     */
    @Query("SELECT vp FROM VendorProduct vp JOIN FETCH vp.vendor JOIN FETCH vp.product WHERE vp.product.id IN :productIds AND vp.isActive = true AND vp.isDeleted = false")
    List<VendorProduct> findByProductIdIn(@Param("productIds") List<String> productIds);
}