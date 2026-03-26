package com.mirror.product.repository;

import com.mirror.product.entity.StoneWeightOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoneWeightOptionRepository extends JpaRepository<StoneWeightOption, Long> {
    Optional<StoneWeightOption> findByWeightCode(String weightCode);
    List<StoneWeightOption> findByIsActiveTrueOrderByDisplayOrder();
    List<StoneWeightOption> findAllByOrderByDisplayOrder();
}
