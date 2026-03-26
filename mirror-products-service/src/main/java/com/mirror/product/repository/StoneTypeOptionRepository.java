package com.mirror.product.repository;

import com.mirror.product.entity.StoneTypeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoneTypeOptionRepository extends JpaRepository<StoneTypeOption, Long> {
    Optional<StoneTypeOption> findByTypeCode(String code);
    List<StoneTypeOption> findByIsActiveTrueOrderByDisplayOrder();
    List<StoneTypeOption> findAllByOrderByDisplayOrder();
}
