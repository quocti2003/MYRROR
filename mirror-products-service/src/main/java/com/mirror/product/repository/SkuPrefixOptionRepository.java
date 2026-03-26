package com.mirror.product.repository;

import com.mirror.product.entity.SkuPrefixOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkuPrefixOptionRepository extends JpaRepository<SkuPrefixOption, Long> {
    Optional<SkuPrefixOption> findByPrefixCode(String prefixCode);
    List<SkuPrefixOption> findByIsActiveTrueOrderByDisplayOrder();
    List<SkuPrefixOption> findAllByOrderByDisplayOrder();
}
