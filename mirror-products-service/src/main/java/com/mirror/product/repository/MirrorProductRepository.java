package com.mirror.product.repository;

import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface MirrorProductRepository extends JpaRepository<MirrorProduct, String> {
    Optional<MirrorProduct> findBySkuCode(String skuCode);
    List<MirrorProduct> findBySkuCodeIn(List<String> skuCodes);
    Optional<MirrorProduct> findByMisaItemCode(String misaItemCode);
    Optional<MirrorProduct> findByDescriptiveCode(String descriptiveCode);
    Optional<MirrorProduct> findByBarcode(String barcode);
    List<MirrorProduct> findByCategory(String category);

    // Status-based queries
    long countByStatus(ProductStatus status);
    List<MirrorProduct> findByStatus(ProductStatus status);

    @Query("SELECT p FROM MirrorProduct p WHERE p.stockQuantity <= p.minStockLevel AND p.minStockLevel IS NOT NULL")
    List<MirrorProduct> findLowStockProducts();

    // Optimized queries for active/published products
    @Query("SELECT p FROM MirrorProduct p WHERE p.isActive = true AND p.isDeleted = false ORDER BY p.createdAt DESC")
    List<MirrorProduct> findAllActive();

    @Query("SELECT p FROM MirrorProduct p WHERE p.status = 'PUBLISHED' AND p.isActive = true AND p.isDeleted = false ORDER BY p.createdAt DESC")
    List<MirrorProduct> findAllPublished();

    // SKU-related queries (for SkuService compatibility)
    @Query("SELECT p FROM MirrorProduct p WHERE p.id = :id AND p.isActive = true AND p.isDeleted = false")
    Optional<MirrorProduct> findActiveById(String id);

    @Query("SELECT p FROM MirrorProduct p WHERE p.itemName = :skuName AND p.isActive = true AND p.isDeleted = false")
    Optional<MirrorProduct> findActiveBySkuName(String skuName);

    @Query("SELECT COUNT(p) > 0 FROM MirrorProduct p WHERE p.itemName = :skuName AND p.isActive = true AND p.isDeleted = false")
    boolean existsActiveBySkuName(String skuName);

    @Query("SELECT COUNT(p) FROM MirrorProduct p WHERE p.isActive = true AND p.isDeleted = false")
    long countActive();

    @Query("SELECT COUNT(p) > 0 FROM MirrorProduct p WHERE p.itemName = :skuName AND p.id != :id AND p.isActive = true AND p.isDeleted = false")
    boolean existsActiveBySkuNameAndNotId(String skuName, String id);

    @Query("SELECT COUNT(p) > 0 FROM MirrorProduct p WHERE p.skuCode = :skuCode AND p.id != :id")
    boolean existsBySkuCodeAndNotId(String skuCode, String id);

    @Query("SELECT COUNT(p) > 0 FROM MirrorProduct p WHERE p.skuCode = :skuCode")
    boolean existsBySkuCode(String skuCode);
}
