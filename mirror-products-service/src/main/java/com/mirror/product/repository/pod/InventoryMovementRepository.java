package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.InventoryMovement;
import com.mirror.product.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends BaseRepository<InventoryMovement, String> {

    Page<InventoryMovement> findByPartnerIdAndIsDeletedFalseOrderByCreatedAtDesc(
        String partnerId, Pageable pageable);

    Page<InventoryMovement> findByInventoryIdAndIsDeletedFalseOrderByCreatedAtDesc(
        String inventoryId, Pageable pageable);

    List<InventoryMovement> findByPartnerIdAndCreatedAtBetweenAndIsDeletedFalse(
        String partnerId, Instant start, Instant end);
}
