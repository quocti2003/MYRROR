package com.mirror.product.repository;

import com.mirror.product.entity.ProductCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCertificateRepository extends JpaRepository<ProductCertificate, Long> {

    /**
     * Find all product-certificate relationships for a product
     */
    List<ProductCertificate> findByProductId(String productId);

    /**
     * Find all product-certificate relationships for a certificate
     */
    List<ProductCertificate> findByCertificateId(String certificateId);

    /**
     * Delete all certificates for a product
     */
    @Modifying
    @Query("DELETE FROM ProductCertificate pc WHERE pc.product.id = :productId")
    void deleteByProductId(@Param("productId") String productId);

    /**
     * Check if a product-certificate relationship exists
     */
    boolean existsByProductIdAndCertificateId(String productId, String certificateId);

    /**
     * Get certificate codes for a product
     */
    @Query("SELECT pc.certificate.certificateCode FROM ProductCertificate pc WHERE pc.product.id = :productId")
    List<String> findCertificateCodesByProductId(@Param("productId") String productId);

    /**
     * Batch load certificate codes for multiple product IDs (for N+1 optimization)
     */
    @Query("SELECT pc FROM ProductCertificate pc JOIN FETCH pc.certificate JOIN FETCH pc.product WHERE pc.product.id IN :productIds")
    List<ProductCertificate> findByProductIdIn(@Param("productIds") List<String> productIds);
}
