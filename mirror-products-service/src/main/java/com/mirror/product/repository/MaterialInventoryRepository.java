package com.mirror.product.repository;

import com.mirror.product.entity.MaterialInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MaterialInventory - Raw materials inventory management
 * Migrated from mirror-mrp-service MaterialRepository
 */
@Repository
public interface MaterialInventoryRepository extends JpaRepository<MaterialInventory, UUID> {

    List<MaterialInventory> findByType(String type);

    List<MaterialInventory> findByVendorId(String vendorId);

    Optional<MaterialInventory> findByName(String name);

    @Query("SELECT m FROM MaterialInventory m WHERE m.vendor.id = :vendorId AND m.type = :type")
    List<MaterialInventory> findByVendorIdAndType(@Param("vendorId") String vendorId, @Param("type") String type);

    @Query("SELECT m FROM MaterialInventory m WHERE m.basePrice <= :maxPrice ORDER BY m.basePrice ASC")
    List<MaterialInventory> findByMaxPrice(@Param("maxPrice") java.math.BigDecimal maxPrice);
}
