package com.mirror.product.repository;

import com.mirror.product.entity.Order;
import com.mirror.product.enums.OrderStatus;
import com.mirror.product.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends BaseRepository<Order, String> {

    @Override
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.isDeleted = false")
    Optional<Order> findActiveById(@Param("id") String id);

    @Query("SELECT o FROM Order o WHERE o.isDeleted = false ORDER BY o.createdAt DESC")
    Page<Order> findAllActive(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.isDeleted = false AND o.status = :status ORDER BY o.createdAt DESC")
    Page<Order> findByStatus(@Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.isDeleted = false AND o.paymentStatus = :paymentStatus ORDER BY o.createdAt DESC")
    Page<Order> findByPaymentStatus(@Param("paymentStatus") PaymentStatus paymentStatus, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.isDeleted = false AND o.status IN :statuses ORDER BY o.createdAt DESC")
    Page<Order> findByStatuses(@Param("statuses") List<OrderStatus> statuses, Pageable pageable);

    List<Order> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId);

    @Query("SELECT o FROM Order o WHERE o.isDeleted = false AND o.createdAt BETWEEN :start AND :end ORDER BY o.createdAt DESC")
    List<Order> findByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

    Page<Order> findByVendorIdAndIsDeletedFalseOrderByCreatedAtDesc(String vendorId, Pageable pageable);

    List<Order> findByVendorIdAndIsDeletedFalseOrderByCreatedAtDesc(String vendorId);

    // New production flow query methods
    @Query("SELECT o FROM Order o WHERE o.isDeleted = false AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByStatusOrderByCreatedAtDesc(@Param("status") OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.isDeleted = false AND o.status = :status AND o.misaItemCreated = :misaItemCreated ORDER BY o.shippedAt ASC")
    List<Order> findByStatusAndMisaItemCreatedOrderByShippedAtAsc(@Param("status") OrderStatus status, @Param("misaItemCreated") Boolean misaItemCreated);

    @Query("SELECT o FROM Order o WHERE o.isDeleted = false AND o.status = :status AND o.misaSaleRecorded = :misaSaleRecorded ORDER BY o.completedAt ASC")
    List<Order> findByStatusAndMisaSaleRecordedOrderByCompletedAtAsc(@Param("status") OrderStatus status, @Param("misaSaleRecorded") Boolean misaSaleRecorded);

    @Query("SELECT o FROM Order o WHERE o.isDeleted = false AND o.status = :status AND o.misaItemCreated = :misaItemCreated ORDER BY o.confirmedAt ASC")
    List<Order> findByStatusAndMisaItemCreatedOrderByConfirmedAtAsc(@Param("status") OrderStatus status, @Param("misaItemCreated") Boolean misaItemCreated);

    @Query("SELECT o FROM Order o WHERE o.isDeleted = false AND o.status = 'CONFIRMED' AND o.misaItemCreated = false AND o.misaSkuRetryCount < :maxRetries ORDER BY o.confirmedAt ASC")
    List<Order> findConfirmedOrdersAwaitingSkuCreation(@Param("maxRetries") int maxRetries);

    @Query("SELECT o FROM Order o WHERE o.isDeleted = false AND o.paymentStatus = 'PAID' AND o.misaSaleRecorded = false AND o.misaInvoiceRetryCount < :maxRetries ORDER BY o.createdAt ASC")
    List<Order> findPaidOrdersAwaitingInvoice(@Param("maxRetries") int maxRetries);
}
