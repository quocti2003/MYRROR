package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.PodPartner;
import com.mirror.product.enums.PartnerStatus;
import com.mirror.product.enums.PartnerTier;
import com.mirror.product.enums.PartnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.mirror.product.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PodPartnerRepository extends BaseRepository<PodPartner, String>, JpaSpecificationExecutor<PodPartner> {

    Optional<PodPartner> findByContactEmail(String contactEmail);

    boolean existsByContactEmail(String contactEmail);

    Optional<PodPartner> findByUserId(Long userId);

    List<PodPartner> findByStatusAndIsDeletedFalse(PartnerStatus status);

    List<PodPartner> findByTierAndIsDeletedFalse(PartnerTier tier);

    Page<PodPartner> findByStatusAndIsDeletedFalse(PartnerStatus status, Pageable pageable);

    Page<PodPartner> findByTierAndIsDeletedFalse(PartnerTier tier, Pageable pageable);

    @Query("SELECT p FROM PodPartner p WHERE p.status = :status AND p.tier = :tier AND p.isDeleted = false")
    Page<PodPartner> findByStatusAndTier(@Param("status") PartnerStatus status, @Param("tier") PartnerTier tier, Pageable pageable);

    @Query("SELECT p FROM PodPartner p WHERE p.city = :city AND p.isDeleted = false")
    List<PodPartner> findByCity(@Param("city") String city);

    @Query("SELECT COUNT(p) FROM PodPartner p WHERE p.status = :status AND p.isDeleted = false")
    long countByStatus(@Param("status") PartnerStatus status);

    @Query("SELECT p FROM PodPartner p WHERE p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<PodPartner> findAllActive(Pageable pageable);

    @Query("SELECT p FROM PodPartner p WHERE " +
           "(LOWER(p.businessName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.contactEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.contactName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND p.isDeleted = false")
    Page<PodPartner> searchByKeyword(@Param("search") String search, Pageable pageable);

    Page<PodPartner> findByPartnerTypeAndIsDeletedFalse(PartnerType partnerType, Pageable pageable);

    @Query("SELECT COUNT(p) FROM PodPartner p WHERE p.partnerType = :type AND p.status = :status AND p.isDeleted = false")
    long countByPartnerTypeAndStatus(@Param("type") PartnerType type, @Param("status") PartnerStatus status);
}
