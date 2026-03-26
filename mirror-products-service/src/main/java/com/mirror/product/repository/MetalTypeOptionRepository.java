package com.mirror.product.repository;

import com.mirror.product.entity.MetalTypeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetalTypeOptionRepository extends JpaRepository<MetalTypeOption, Long> {
    Optional<MetalTypeOption> findByTypeCode(String code);
    List<MetalTypeOption> findByIsActiveTrueOrderByDisplayOrder();
    List<MetalTypeOption> findAllByOrderByDisplayOrder();
}
