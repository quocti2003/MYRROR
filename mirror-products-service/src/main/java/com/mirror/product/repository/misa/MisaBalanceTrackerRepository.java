package com.mirror.product.repository.misa;

import com.mirror.product.entity.misa.MisaBalanceTracker;
import com.mirror.product.entity.misa.MisaBalanceTracker.BalanceTrackerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MisaBalanceTrackerRepository extends JpaRepository<MisaBalanceTracker, Long> {

    List<MisaBalanceTracker> findByStatus(BalanceTrackerStatus status);

    List<MisaBalanceTracker> findByStatusAndTimeoutAtBefore(BalanceTrackerStatus status, LocalDateTime dateTime);

    List<MisaBalanceTracker> findByOrgRefId(String orgRefId);

    List<MisaBalanceTracker> findByInventoryItemCodeAndStatus(String inventoryItemCode, BalanceTrackerStatus status);

    List<MisaBalanceTracker> findTop50ByOrderByCreatedAtDesc();

    Page<MisaBalanceTracker> findByStatus(BalanceTrackerStatus status, Pageable pageable);

    Page<MisaBalanceTracker> findAll(Pageable pageable);
}
