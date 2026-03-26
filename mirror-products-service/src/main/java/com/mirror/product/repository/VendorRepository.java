package com.mirror.product.repository;

import com.mirror.product.entity.Vendor;
import com.mirror.product.enums.VendorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends BaseRepository<Vendor, String> {
    
    @Override
    @Query("SELECT v FROM Vendor v WHERE v.id = :id AND v.isActive = true AND v.isDeleted = false")
    Optional<Vendor> findActiveById(@Param("id") String id);
    
    @Override
    @Query("SELECT COUNT(v) > 0 FROM Vendor v WHERE v.id = :id AND v.isActive = true AND v.isDeleted = false")
    boolean existsActiveById(@Param("id") String id);
    
    @Query("SELECT v FROM Vendor v WHERE v.code = :code AND v.isActive = true AND v.isDeleted = false")
    Optional<Vendor> findActiveByCode(@Param("code") String code);
    
    @Query("SELECT COUNT(v) > 0 FROM Vendor v WHERE v.code = :code AND v.isActive = true AND v.isDeleted = false")
    boolean existsActiveByCode(@Param("code") String code);
    
    @Query("SELECT COUNT(v) > 0 FROM Vendor v WHERE v.code = :code AND v.id != :vendorId AND v.isActive = true AND v.isDeleted = false")
    boolean existsActiveByCodeAndNotId(@Param("code") String code, @Param("vendorId") String vendorId);
    
    @Query("SELECT v FROM Vendor v WHERE v.country = :country AND v.isActive = true AND v.isDeleted = false")
    List<Vendor> findActiveByCountry(@Param("country") String country);
    
    @Query("SELECT v FROM Vendor v WHERE v.vendorType = :vendorType AND v.isActive = true AND v.isDeleted = false")
    List<Vendor> findActiveByVendorType(@Param("vendorType") VendorType vendorType);
    
    @Query("SELECT v FROM Vendor v WHERE v.ownerUserId = :ownerUserId AND v.isActive = true AND v.isDeleted = false")
    List<Vendor> findActiveByOwnerUserId(@Param("ownerUserId") String ownerUserId);
    
    @Query("SELECT v FROM Vendor v WHERE v.isActive = true AND v.isDeleted = false AND " +
           "(LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Vendor> searchActiveVendors(@Param("search") String search, Pageable pageable);
}