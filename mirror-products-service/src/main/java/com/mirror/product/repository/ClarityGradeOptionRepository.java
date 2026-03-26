package com.mirror.product.repository;

import com.mirror.product.entity.ClarityGradeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClarityGradeOptionRepository extends JpaRepository<ClarityGradeOption, Long> {
    Optional<ClarityGradeOption> findByClarityCode(String code);
    List<ClarityGradeOption> findByIsActiveTrueOrderByDisplayOrder();
    List<ClarityGradeOption> findAllByOrderByDisplayOrder();
}
