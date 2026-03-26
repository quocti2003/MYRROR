package com.mirror.product.repository;

import com.mirror.product.entity.OrderPaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderPaymentTransactionRepository extends JpaRepository<OrderPaymentTransaction, String> {

    List<OrderPaymentTransaction> findByScheduleIdOrderByPaidAtDesc(String scheduleId);

    List<OrderPaymentTransaction> findByOrderIdOrderByPaidAtDesc(String orderId);
}
