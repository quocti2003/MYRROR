package com.mirror.product.repository;

import com.mirror.product.entity.MirrorDiamond;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Mirror Diamonds with Mirror SKU system
 * SKU Format: LGD-{COLOR}{CLARITY}-{CARAT}-{SHAPE}-{ORIGIN}-{MFR}-{CERT}
 */
@Repository
public interface MirrorDiamondRepository extends JpaRepository<MirrorDiamond, String> {

    // ==================== SKU OPERATIONS ====================

    Optional<MirrorDiamond> findBySkuCode(String skuCode);

    boolean existsBySkuCode(String skuCode);

    Optional<MirrorDiamond> findByCertNumber(String certNumber);

    boolean existsByCertNumber(String certNumber);

    // ==================== FILTER BY SPECIFICATIONS ====================

    List<MirrorDiamond> findByColor(String color);

    List<MirrorDiamond> findByClarity(String clarity);

    List<MirrorDiamond> findByColorAndClarity(String color, String clarity);

    List<MirrorDiamond> findByShape(String shape);

    @Query("SELECT d FROM MirrorDiamond d WHERE d.caratWeight BETWEEN :minCarat AND :maxCarat")
    List<MirrorDiamond> findByCaratWeightBetween(
            @Param("minCarat") BigDecimal minCarat,
            @Param("maxCarat") BigDecimal maxCarat);

    // ==================== FILTER BY STATUS ====================

    List<MirrorDiamond> findByStatus(String status);

    List<MirrorDiamond> findByStatusAndIsActiveTrue(String status);

    @Query("SELECT d FROM MirrorDiamond d WHERE d.isActive = true ORDER BY d.createdAt DESC")
    List<MirrorDiamond> findAllActiveOrderByCreatedAtDesc();

    // ==================== FILTER BY MANUFACTURER ====================

    List<MirrorDiamond> findByManufacturerCode(String manufacturerCode);

    List<MirrorDiamond> findByManufacturerName(String manufacturerName);

    // ==================== FILTER BY INVOICE ====================

    List<MirrorDiamond> findByInvoiceNumber(String invoiceNumber);

    @Query("SELECT d FROM MirrorDiamond d WHERE d.invoiceNumber = :invoiceNumber ORDER BY d.skuCode")
    List<MirrorDiamond> findByInvoiceNumberOrderBySkuCode(@Param("invoiceNumber") String invoiceNumber);

    // ==================== COMPLEX SEARCH QUERY ====================

    @Query("SELECT d FROM MirrorDiamond d WHERE " +
           "(:color IS NULL OR d.color = :color) AND " +
           "(:clarity IS NULL OR d.clarity = :clarity) AND " +
           "(:shape IS NULL OR d.shape = :shape) AND " +
           "(:minCarat IS NULL OR d.caratWeight >= :minCarat) AND " +
           "(:maxCarat IS NULL OR d.caratWeight <= :maxCarat) AND " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:manufacturerCode IS NULL OR d.manufacturerCode = :manufacturerCode) AND " +
           "d.isActive = true " +
           "ORDER BY d.caratWeight DESC")
    List<MirrorDiamond> searchDiamonds(
            @Param("color") String color,
            @Param("clarity") String clarity,
            @Param("shape") String shape,
            @Param("minCarat") BigDecimal minCarat,
            @Param("maxCarat") BigDecimal maxCarat,
            @Param("status") String status,
            @Param("manufacturerCode") String manufacturerCode);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(d) FROM MirrorDiamond d WHERE d.status = :status AND d.isActive = true")
    long countByStatus(@Param("status") String status);

    @Query("SELECT SUM(d.caratWeight) FROM MirrorDiamond d WHERE d.status = 'IN_STOCK' AND d.isActive = true")
    BigDecimal getTotalCaratWeightInStock();

    @Query("SELECT SUM(d.totalPriceUsd) FROM MirrorDiamond d WHERE d.status = 'IN_STOCK' AND d.isActive = true")
    BigDecimal getTotalInventoryValueUsd();
}
