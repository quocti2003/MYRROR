package com.mirror.product.repository;

import com.mirror.product.entity.OrderPaymentSchedule;
import com.mirror.product.enums.PaymentScheduleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderPaymentScheduleRepository extends BaseRepository<OrderPaymentSchedule, String> {

    List<OrderPaymentSchedule> findByOrder_IdOrderByDueDateAsc(String orderId);

    Optional<OrderPaymentSchedule> findByIdAndOrder_Id(String id, String orderId);

    @Query("SELECT ps FROM OrderPaymentSchedule ps WHERE ps.order.id = :orderId AND ps.status = :status")
    List<OrderPaymentSchedule> findByOrderAndStatus(@Param("orderId") String orderId,
                                                    @Param("status") PaymentScheduleStatus status);

    @Query("SELECT ps FROM OrderPaymentSchedule ps WHERE ps.order.id = :orderId AND ps.dueDate < :threshold AND ps.amountDue > ps.amountPaid")
    List<OrderPaymentSchedule> findOverdueEntries(@Param("orderId") String orderId, @Param("threshold") Instant threshold);

    @Query("SELECT ps FROM OrderPaymentSchedule ps WHERE ps.order.isDeleted = false ORDER BY ps.dueDate ASC")
    Page<OrderPaymentSchedule> findAllActive(Pageable pageable);

    @Query("SELECT ps FROM OrderPaymentSchedule ps WHERE ps.order.isDeleted = false AND ps.status = :status ORDER BY ps.dueDate ASC")
    Page<OrderPaymentSchedule> findByStatus(@Param("status") PaymentScheduleStatus status, Pageable pageable);

    @Query("SELECT DISTINCT ps.order.id FROM OrderPaymentSchedule ps WHERE ps.dueDate < :now AND ps.amountPaid < ps.amountDue AND ps.status != 'PAID' AND ps.order.isDeleted = false")
    List<String> findOrderIdsWithOverdueSchedules(@Param("now") Instant now);
}
