package com.mirror.product.repository.misa;

import com.mirror.product.entity.misa.MisaCustomer;
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
public interface MisaCustomerRepository extends JpaRepository<MisaCustomer, Long> {

    Optional<MisaCustomer> findByCustomerId(String customerId);

    Optional<MisaCustomer> findByCustomerCode(String customerCode);

    Page<MisaCustomer> findByIsActiveTrue(Pageable pageable);

    Page<MisaCustomer> findByCustomerNameContaining(String name, Pageable pageable);

    List<MisaCustomer> findByCustomerNameContainingOrPhoneContaining(String name, String phone);

    Optional<MisaCustomer> findFirstByPhone(String phone);

    Optional<MisaCustomer> findFirstByNormalizedPhone(String normalizedPhone);

    @Query("SELECT c FROM MisaCustomer c WHERE c.lastSyncDate IS NULL OR c.lastSyncDate < :cutoffTime")
    List<MisaCustomer> findCustomersNeedingSync(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT COUNT(c) FROM MisaCustomer c WHERE c.isActive = true")
    Long countActiveCustomers();

    @Query("SELECT DISTINCT c.memberLevelName FROM MisaCustomer c WHERE c.memberLevelName IS NOT NULL ORDER BY c.memberLevelName")
    List<String> findDistinctMemberLevels();

    @Query("SELECT c FROM MisaCustomer c WHERE c.email IS NOT NULL AND c.email <> ''")
    List<MisaCustomer> findCustomersWithEmail();

    @Query("SELECT c FROM MisaCustomer c WHERE c.membershipCode IS NOT NULL AND c.membershipCode <> ''")
    List<MisaCustomer> findMembershipCustomers();
}
