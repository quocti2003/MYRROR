package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.WholesaleOrderItem;
import com.mirror.product.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WholesaleOrderItemRepository extends BaseRepository<WholesaleOrderItem, String> {

    List<WholesaleOrderItem> findByOrderIdAndIsDeletedFalse(String orderId);
}
