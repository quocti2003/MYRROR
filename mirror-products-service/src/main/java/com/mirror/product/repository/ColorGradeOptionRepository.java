package com.mirror.product.repository;

import com.mirror.product.entity.ColorGradeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColorGradeOptionRepository extends JpaRepository<ColorGradeOption, Long> {
    Optional<ColorGradeOption> findByGradeCode(String code);
    List<ColorGradeOption> findByIsActiveTrueOrderByDisplayOrder();
    List<ColorGradeOption> findAllByOrderByDisplayOrder();
}
