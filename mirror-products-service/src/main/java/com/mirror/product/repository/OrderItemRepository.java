package com.mirror.product.repository;

import com.mirror.product.entity.OrderItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends BaseRepository<OrderItem, String> {

    List<OrderItem> findByOrder_Id(String orderId);
}
