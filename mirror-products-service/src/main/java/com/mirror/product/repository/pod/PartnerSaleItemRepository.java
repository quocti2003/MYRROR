package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.PartnerSaleItem;
import com.mirror.product.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerSaleItemRepository extends BaseRepository<PartnerSaleItem, String> {

    List<PartnerSaleItem> findBySaleIdAndIsDeletedFalse(String saleId);
}
