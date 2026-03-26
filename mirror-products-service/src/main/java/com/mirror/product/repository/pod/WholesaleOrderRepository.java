package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.WholesaleOrder;
import com.mirror.product.enums.WholesaleOrderStatus;
import com.mirror.product.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WholesaleOrderRepository extends BaseRepository<WholesaleOrder, String>,
        JpaSpecificationExecutor<WholesaleOrder> {

    Optional<WholesaleOrder> findByIdAndIsDeletedFalse(String id);

    Optional<WholesaleOrder> findByIdAndPartnerIdAndIsDeletedFalse(String id, String partnerId);

    Optional<WholesaleOrder> findByOrderNumberAndIsDeletedFalse(String orderNumber);

    Page<WholesaleOrder> findByPartnerIdAndIsDeletedFalseOrderByCreatedAtDesc(
        String partnerId, Pageable pageable);

    Page<WholesaleOrder> findByStatusAndIsDeletedFalse(WholesaleOrderStatus status, Pageable pageable);

    Page<WholesaleOrder> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(wo) FROM WholesaleOrder wo WHERE wo.partnerId = :partnerId " +
           "AND wo.status = :status AND wo.isDeleted = false")
    long countByPartnerAndStatus(@Param("partnerId") String partnerId, @Param("status") WholesaleOrderStatus status);

    @Query("SELECT COALESCE(SUM(wo.totalAmount), 0) FROM WholesaleOrder wo " +
           "WHERE wo.partnerId = :partnerId AND wo.status = :status AND wo.isDeleted = false")
    BigDecimal calculateTotalPurchased(@Param("partnerId") String partnerId, @Param("status") WholesaleOrderStatus status);
}
