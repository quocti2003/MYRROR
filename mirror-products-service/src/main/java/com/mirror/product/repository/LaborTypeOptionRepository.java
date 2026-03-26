package com.mirror.product.repository;

import com.mirror.product.entity.LaborTypeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LaborTypeOptionRepository extends JpaRepository<LaborTypeOption, Long> {
    Optional<LaborTypeOption> findByLaborCode(String code);
    List<LaborTypeOption> findByIsActiveTrueOrderByDisplayOrder();
    List<LaborTypeOption> findAllByOrderByDisplayOrder();
}
