package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.Pod;
import com.mirror.product.enums.PodStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.mirror.product.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PodRepository extends BaseRepository<Pod, String>, JpaSpecificationExecutor<Pod> {

    List<Pod> findByPartnerId(String partnerId);

    Page<Pod> findByPartnerIdAndIsDeletedFalse(String partnerId, Pageable pageable);

    List<Pod> findByStatus(PodStatus status);

    Page<Pod> findByStatusAndIsDeletedFalse(PodStatus status, Pageable pageable);

    @Query("SELECT p FROM Pod p WHERE p.partnerId = :partnerId AND p.status = :status AND p.isDeleted = false")
    List<Pod> findByPartnerIdAndStatus(@Param("partnerId") String partnerId, @Param("status") PodStatus status);

    @Query("SELECT p FROM Pod p WHERE p.city = :city AND p.isDeleted = false")
    List<Pod> findByCity(@Param("city") String city);

    @Query("SELECT COUNT(p) FROM Pod p WHERE p.partnerId = :partnerId AND p.isDeleted = false")
    long countByPartnerId(@Param("partnerId") String partnerId);

    @Query("SELECT COUNT(p) FROM Pod p WHERE p.status = :status AND p.isDeleted = false")
    long countByStatus(@Param("status") PodStatus status);

    @Query("SELECT p FROM Pod p WHERE p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Pod> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Pod p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.locationName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.city) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND p.isDeleted = false")
    Page<Pod> searchByKeyword(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Pod p JOIN p.products prod WHERE prod.id = :productId AND p.isDeleted = false")
    List<Pod> findByProductId(@Param("productId") String productId);

    @Query("SELECT DISTINCT p.city FROM Pod p WHERE p.city IS NOT NULL AND p.isDeleted = false ORDER BY p.city")
    List<String> findDistinctCities();
}
