package com.mirror.product.repository;

import com.mirror.product.entity.Designer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignerRepository extends BaseRepository<Designer, String> {

    @Override
    @Query("SELECT d FROM Designer d WHERE d.id = :id AND d.isActive = true AND d.isDeleted = false")
    Optional<Designer> findActiveById(@Param("id") String id);

    @Override
    @Query("SELECT COUNT(d) > 0 FROM Designer d WHERE d.id = :id AND d.isActive = true AND d.isDeleted = false")
    boolean existsActiveById(@Param("id") String id);

    @Query("SELECT d FROM Designer d WHERE d.code = :code AND d.isActive = true AND d.isDeleted = false")
    Optional<Designer> findActiveByCode(@Param("code") String code);

    @Query("SELECT COUNT(d) > 0 FROM Designer d WHERE d.code = :code AND d.isActive = true AND d.isDeleted = false")
    boolean existsActiveByCode(@Param("code") String code);

    @Query("SELECT COUNT(d) > 0 FROM Designer d WHERE d.code = :code AND d.id != :designerId AND d.isActive = true AND d.isDeleted = false")
    boolean existsActiveByCodeAndNotId(@Param("code") String code, @Param("designerId") String designerId);

    @Query("SELECT d FROM Designer d WHERE d.ownerUserId = :ownerUserId AND d.isActive = true AND d.isDeleted = false")
    List<Designer> findActiveByOwnerUserId(@Param("ownerUserId") String ownerUserId);

    @Query("SELECT d FROM Designer d WHERE d.specialty = :specialty AND d.isActive = true AND d.isDeleted = false")
    List<Designer> findActiveBySpecialty(@Param("specialty") String specialty);

    @Query("SELECT d FROM Designer d WHERE d.verified = true AND d.isActive = true AND d.isDeleted = false")
    List<Designer> findActiveVerifiedDesigners();

    @Query("SELECT d FROM Designer d WHERE d.featured = true AND d.isActive = true AND d.isDeleted = false")
    List<Designer> findActiveFeaturedDesigners();

    @Query("SELECT d FROM Designer d WHERE d.isActive = true AND d.isDeleted = false AND " +
           "(LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.brandName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Designer> searchActiveDesigners(@Param("search") String search, Pageable pageable);
}