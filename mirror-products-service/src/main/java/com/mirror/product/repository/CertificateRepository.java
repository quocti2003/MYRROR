package com.mirror.product.repository;

import com.mirror.product.entity.Certificate;
import com.mirror.product.enums.CertificateType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends BaseRepository<Certificate, String> {

    /**
     * Find certificate by certificate code
     */
    @Query("SELECT c FROM Certificate c WHERE c.certificateCode = :code AND c.isDeleted = false")
    Optional<Certificate> findByCertificateCode(@Param("code") String code);

    /**
     * Find certificates by type
     */
    @Query("SELECT c FROM Certificate c WHERE c.certificateType = :type AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Certificate> findByType(@Param("type") CertificateType type);

    /**
     * Check if certificate code already exists
     */
    @Query("SELECT COUNT(c) > 0 FROM Certificate c WHERE c.certificateCode = :code AND c.isDeleted = false")
    boolean existsByCertificateCode(@Param("code") String code);

    /**
     * Check if certificate code already exists (excluding a specific ID for update scenarios)
     */
    @Query("SELECT COUNT(c) > 0 FROM Certificate c WHERE c.certificateCode = :code AND c.id != :id AND c.isDeleted = false")
    boolean existsByCertificateCodeAndIdNot(@Param("code") String code, @Param("id") String id);
}
