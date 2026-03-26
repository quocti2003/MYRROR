package com.mirror.product.repository;

import com.mirror.product.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, String> {

    /**
     * Find all active locations
     */
    List<Location> findByStatusOrderByCreatedAtDesc(Location.LocationStatus status);

    /**
     * Find locations by city
     */
    List<Location> findByCityAndStatusOrderByCreatedAtDesc(String city, Location.LocationStatus status);

    /**
     * Find locations by type
     */
    List<Location> findByTypeAndStatusOrderByCreatedAtDesc(Location.LocationType type, Location.LocationStatus status);

    /**
     * Find locations by city and type
     */
    List<Location> findByCityAndTypeAndStatusOrderByCreatedAtDesc(
            String city, Location.LocationType type, Location.LocationStatus status);

    /**
     * Find all distinct cities with active locations
     */
    @Query("SELECT DISTINCT l.city FROM Location l WHERE l.status = :status ORDER BY l.city")
    List<String> findDistinctCitiesByStatus(@Param("status") Location.LocationStatus status);

    /**
     * Find all distinct types with active locations
     */
    @Query("SELECT DISTINCT l.type FROM Location l WHERE l.status = :status ORDER BY l.type")
    List<Location.LocationType> findDistinctTypesByStatus(@Param("status") Location.LocationStatus status);

    /**
     * Search locations by name (case-insensitive)
     */
    @Query("SELECT l FROM Location l WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND l.status = :status ORDER BY l.createdAt DESC")
    List<Location> searchByNameAndStatus(@Param("searchTerm") String searchTerm, 
                                        @Param("status") Location.LocationStatus status);

    /**
     * Search locations by name or city (case-insensitive)
     */
    @Query("SELECT l FROM Location l WHERE " +
           "(LOWER(l.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(l.city) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(l.address) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND l.status = :status ORDER BY l.createdAt DESC")
    List<Location> searchByTextAndStatus(@Param("searchTerm") String searchTerm, 
                                        @Param("status") Location.LocationStatus status);

    /**
     * Count locations by status
     */
    long countByStatus(Location.LocationStatus status);

    /**
     * Count locations by city and status
     */
    long countByCityAndStatus(String city, Location.LocationStatus status);

    /**
     * Count locations by type and status
     */
    long countByTypeAndStatus(Location.LocationType type, Location.LocationStatus status);

    // ============ Warehouse-specific queries ============

    /**
     * Find all warehouses (type = WAREHOUSE)
     */
    @Query("SELECT l FROM Location l WHERE l.type = 'WAREHOUSE' AND l.status = :status " +
           "ORDER BY l.name")
    List<Location> findWarehouses(@Param("status") Location.LocationStatus status);

    /**
     * Find all active warehouses
     */
    @Query("SELECT l FROM Location l WHERE l.type = 'WAREHOUSE' AND l.status = 'ACTIVE' " +
           "AND l.isDeleted = false ORDER BY l.name")
    List<Location> findActiveWarehouses();

    /**
     * Find warehouse by MISA warehouse ID
     */
    @Query("SELECT l FROM Location l WHERE l.misaWarehouseId = :misaId AND l.isDeleted = false")
    java.util.Optional<Location> findByMisaWarehouseId(@Param("misaId") String misaWarehouseId);

    /**
     * Find warehouse by MISA warehouse code
     */
    @Query("SELECT l FROM Location l WHERE l.misaWarehouseCode = :misaCode AND l.isDeleted = false")
    java.util.Optional<Location> findByMisaWarehouseCode(@Param("misaCode") String misaWarehouseCode);

    /**
     * Find all warehouses that need MISA sync (never synced or synced before given time)
     */
    @Query("SELECT l FROM Location l WHERE l.type = 'WAREHOUSE' AND l.status = 'ACTIVE' " +
           "AND (l.misaLastSyncedAt IS NULL OR l.misaLastSyncedAt < :syncThreshold) " +
           "ORDER BY l.misaLastSyncedAt NULLS FIRST")
    List<Location> findWarehousesNeedingSync(@Param("syncThreshold") java.time.Instant syncThreshold);

    /**
     * Find customer-facing locations (not internal/warehouse)
     */
    @Query("SELECT l FROM Location l WHERE (l.isInternal = false OR l.isInternal IS NULL) " +
           "AND l.status = :status ORDER BY l.createdAt DESC")
    List<Location> findCustomerFacingLocations(@Param("status") Location.LocationStatus status);

    /**
     * Count warehouses
     */
    @Query("SELECT COUNT(l) FROM Location l WHERE l.type = 'WAREHOUSE' AND l.status = 'ACTIVE'")
    long countActiveWarehouses();
}