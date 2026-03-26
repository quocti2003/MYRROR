package com.mirror.product.repository;

import com.mirror.product.entity.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, String> {

    /**
     * Find all active collections ordered by sort order
     */
    List<Collection> findByStatusOrderBySortOrderAsc(Collection.CollectionStatus status);

    /**
     * Find featured collections
     */
    List<Collection> findByFeaturedTrueAndStatusOrderBySortOrderAsc(Collection.CollectionStatus status);

    /**
     * Find collection by name
     */
    Optional<Collection> findByNameAndStatus(String name, Collection.CollectionStatus status);

    /**
     * Find collections by season and year
     */
    List<Collection> findBySeasonAndYearAndStatusOrderBySortOrderAsc(
            String season, Integer year, Collection.CollectionStatus status);

    /**
     * Find collections by year
     */
    List<Collection> findByYearAndStatusOrderBySortOrderAsc(Integer year, Collection.CollectionStatus status);

    /**
     * Search collections by title or description
     */
    @Query("SELECT c FROM Collection c WHERE " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND c.status = :status ORDER BY c.sortOrder ASC")
    List<Collection> searchCollectionsAndStatus(@Param("searchTerm") String searchTerm,
                                               @Param("status") Collection.CollectionStatus status);

    /**
     * Get collections with product count
     */
    @Query("SELECT c FROM Collection c LEFT JOIN c.collectionProducts cp " +
           "WHERE c.status = :status GROUP BY c ORDER BY c.sortOrder ASC")
    List<Collection> findCollectionsWithProductCount(@Param("status") Collection.CollectionStatus status);

    /**
     * Count collections by status
     */
    long countByStatus(Collection.CollectionStatus status);

    /**
     * Count featured collections
     */
    long countByFeaturedTrueAndStatus(Collection.CollectionStatus status);

    /**
     * Find distinct years
     */
    @Query("SELECT DISTINCT c.year FROM Collection c WHERE c.status = :status ORDER BY c.year DESC")
    List<Integer> findDistinctYearsByStatus(@Param("status") Collection.CollectionStatus status);

    /**
     * Find distinct seasons
     */
    @Query("SELECT DISTINCT c.season FROM Collection c WHERE c.status = :status AND c.season IS NOT NULL")
    List<String> findDistinctSeasonsByStatus(@Param("status") Collection.CollectionStatus status);
}