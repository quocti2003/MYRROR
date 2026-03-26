package com.mirror.product.repository;

import com.mirror.product.entity.StoneShapeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoneShapeOptionRepository extends JpaRepository<StoneShapeOption, Long> {
    Optional<StoneShapeOption> findByShapeCode(String shapeCode);
    List<StoneShapeOption> findByIsActiveTrueOrderByDisplayOrder();
    List<StoneShapeOption> findAllByOrderByDisplayOrder();
}
