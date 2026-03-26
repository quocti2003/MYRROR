package com.mirror.product.repository;

import com.mirror.product.entity.SideStonesOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SideStonesOptionRepository extends JpaRepository<SideStonesOption, Long> {
    Optional<SideStonesOption> findByStoneCode(String stoneCode);
    List<SideStonesOption> findByIsActiveTrueOrderByDisplayOrder();
    List<SideStonesOption> findAllByOrderByDisplayOrder();
}
