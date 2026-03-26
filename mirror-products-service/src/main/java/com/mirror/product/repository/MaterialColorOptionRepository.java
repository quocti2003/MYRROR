package com.mirror.product.repository;

import com.mirror.product.entity.MaterialColorOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialColorOptionRepository extends JpaRepository<MaterialColorOption, Long> {
    Optional<MaterialColorOption> findByColorCode(String colorCode);
    List<MaterialColorOption> findByIsActiveTrueOrderByDisplayOrder();
    List<MaterialColorOption> findAllByOrderByDisplayOrder();
}
