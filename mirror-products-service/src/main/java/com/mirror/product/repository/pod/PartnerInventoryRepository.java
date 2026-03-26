package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.PartnerInventory;
import com.mirror.product.repository.BaseRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerInventoryRepository extends BaseRepository<PartnerInventory, String>,
        JpaSpecificationExecutor<PartnerInventory> {

    Optional<PartnerInventory> findByPartnerIdAndProductIdAndIsDeletedFalse(String partnerId, String productId);

    Optional<PartnerInventory> findByIdAndPartnerIdAndIsDeletedFalse(String id, String partnerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pi FROM PartnerInventory pi WHERE pi.partnerId = :partnerId AND pi.productId = :productId AND pi.isDeleted = false")
    Optional<PartnerInventory> findByPartnerIdAndProductIdForUpdate(@Param("partnerId") String partnerId, @Param("productId") String productId);

    Page<PartnerInventory> findByPartnerIdAndIsDeletedFalse(String partnerId, Pageable pageable);

    List<PartnerInventory> findByPartnerIdAndIsDeletedFalse(String partnerId);

    @Query("SELECT pi FROM PartnerInventory pi WHERE pi.partnerId = :partnerId " +
           "AND pi.quantityAvailable <= pi.reorderLevel AND pi.isDeleted = false")
    List<PartnerInventory> findLowStockByPartner(@Param("partnerId") String partnerId);

    @Query("SELECT pi FROM PartnerInventory pi WHERE pi.quantityAvailable <= pi.reorderLevel " +
           "AND pi.isDeleted = false")
    List<PartnerInventory> findAllLowStock();

    @Query("SELECT COUNT(pi) FROM PartnerInventory pi WHERE pi.partnerId = :partnerId AND pi.isDeleted = false")
    long countByPartner(@Param("partnerId") String partnerId);

    @Query("SELECT COALESCE(SUM(pi.quantityOnHand * pi.wholesalePrice), 0) FROM PartnerInventory pi " +
           "WHERE pi.partnerId = :partnerId AND pi.isDeleted = false")
    BigDecimal calculateInventoryValue(@Param("partnerId") String partnerId);
}
