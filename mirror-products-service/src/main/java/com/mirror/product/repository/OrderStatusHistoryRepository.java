package com.mirror.product.repository;

import com.mirror.product.entity.OrderStatusHistory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends BaseRepository<OrderStatusHistory, String> {

    List<OrderStatusHistory> findByOrder_IdOrderByChangedAtDesc(String orderId);
}
