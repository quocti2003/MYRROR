package com.mirror.product.repository;

import com.mirror.product.entity.MetalPurityOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetalPurityOptionRepository extends JpaRepository<MetalPurityOption, Long> {
    Optional<MetalPurityOption> findByPurityCode(String code);
    List<MetalPurityOption> findByIsActiveTrueOrderByDisplayOrder();
    List<MetalPurityOption> findAllByOrderByDisplayOrder();
}
