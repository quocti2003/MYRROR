package com.mirror.product.repository;

import com.mirror.product.entity.PartnerCapability;
import com.mirror.product.enums.PartnerCapabilityType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerCapabilityRepository extends BaseRepository<PartnerCapability, String> {

    @Override
    @Query("SELECT c FROM PartnerCapability c WHERE c.id = :id AND c.isActive = true AND c.isDeleted = false")
    Optional<PartnerCapability> findActiveById(@Param("id") String id);

    @Query("SELECT c FROM PartnerCapability c WHERE c.vendor.id = :vendorId AND c.isActive = true AND c.isDeleted = false ORDER BY c.capabilityType")
    List<PartnerCapability> findActiveByVendorId(@Param("vendorId") String vendorId);

    @Query("SELECT c FROM PartnerCapability c WHERE c.capabilityType = :capabilityType AND c.isActive = true AND c.isDeleted = false")
    List<PartnerCapability> findActiveByCapabilityType(@Param("capabilityType") PartnerCapabilityType capabilityType);

    @Query("SELECT c FROM PartnerCapability c WHERE c.vendor.id = :vendorId AND c.capabilityType = :capabilityType AND c.isActive = true AND c.isDeleted = false")
    Optional<PartnerCapability> findActiveByVendorIdAndCapabilityType(
        @Param("vendorId") String vendorId,
        @Param("capabilityType") PartnerCapabilityType capabilityType
    );

    @Query("SELECT COUNT(c) > 0 FROM PartnerCapability c WHERE c.vendor.id = :vendorId AND c.capabilityType = :capabilityType AND c.isActive = true AND c.isDeleted = false")
    boolean existsActiveByVendorIdAndCapabilityType(
        @Param("vendorId") String vendorId,
        @Param("capabilityType") PartnerCapabilityType capabilityType
    );

    @Query("SELECT DISTINCT c.vendor FROM PartnerCapability c WHERE c.capabilityType = :capabilityType AND c.isActive = true AND c.isDeleted = false AND c.vendor.isActive = true AND c.vendor.isDeleted = false")
    List<com.mirror.product.entity.Vendor> findVendorsByCapabilityType(@Param("capabilityType") PartnerCapabilityType capabilityType);

    @Query("SELECT c FROM PartnerCapability c JOIN FETCH c.vendor WHERE c.capabilityType = :capabilityType AND c.isActive = true AND c.isDeleted = false AND c.vendor.isActive = true AND c.vendor.isDeleted = false ORDER BY c.qualityRating DESC NULLS LAST, c.leadTimeDays ASC NULLS LAST")
    List<PartnerCapability> findActiveByCapabilityTypeWithVendorOrderedByQuality(@Param("capabilityType") PartnerCapabilityType capabilityType);

    @Query("SELECT COUNT(c) FROM PartnerCapability c WHERE c.vendor.id = :vendorId AND c.isActive = true AND c.isDeleted = false")
    long countActiveByVendorId(@Param("vendorId") String vendorId);
}
