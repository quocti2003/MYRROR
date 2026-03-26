package com.mirror.product.repository.misa;

import com.mirror.product.entity.misa.MisaInventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MisaInventoryItemRepository extends JpaRepository<MisaInventoryItem, Long> {

    Optional<MisaInventoryItem> findByInventoryItemCode(String inventoryItemCode);

    Optional<MisaInventoryItem> findByInventoryItemId(String inventoryItemId);

    List<MisaInventoryItem> findByInventoryItemCategoryId(String categoryId);

    List<MisaInventoryItem> findByBrandName(String brandName);

    List<MisaInventoryItem> findBySyncStatus(MisaInventoryItem.SyncStatus syncStatus);

    @Query("SELECT m FROM MisaInventoryItem m WHERE m.inventoryItemName LIKE %:name%")
    List<MisaInventoryItem> findByInventoryItemNameContaining(@Param("name") String name);

    @Query("SELECT m FROM MisaInventoryItem m WHERE m.quantityOnHand < m.minimumStock")
    List<MisaInventoryItem> findLowStockItems();

    @Query("SELECT m FROM MisaInventoryItem m WHERE m.lastSyncDate < :cutoffDate")
    List<MisaInventoryItem> findItemsNeedingSync(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT DISTINCT m.inventoryItemCategoryName FROM MisaInventoryItem m WHERE m.inventoryItemCategoryName IS NOT NULL")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT m.brandName FROM MisaInventoryItem m WHERE m.brandName IS NOT NULL")
    List<String> findDistinctBrands();

    @Query("SELECT COUNT(m) FROM MisaInventoryItem m WHERE m.isActive = true")
    Long countActiveItems();

    Page<MisaInventoryItem> findByIsActiveTrue(Pageable pageable);

    Page<MisaInventoryItem> findByInventoryItemCategoryNameContaining(String categoryName, Pageable pageable);

    boolean existsByInventoryItemCode(String inventoryItemCode);

    /**
     * Search items by name or code (used by ProductOpsService)
     */
    @Query("SELECT m FROM MisaInventoryItem m WHERE " +
           "LOWER(m.inventoryItemName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.inventoryItemCode) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<MisaInventoryItem> searchByNameOrCode(@Param("search") String search);
}
