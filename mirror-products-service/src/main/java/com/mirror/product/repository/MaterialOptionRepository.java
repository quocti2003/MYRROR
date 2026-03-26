package com.mirror.product.repository;

import com.mirror.product.entity.MaterialOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialOptionRepository extends JpaRepository<MaterialOption, Long> {
    Optional<MaterialOption> findByMaterialCode(String materialCode);
    List<MaterialOption> findByIsActiveTrueOrderByDisplayOrder();
    List<MaterialOption> findAllByOrderByDisplayOrder();
}
