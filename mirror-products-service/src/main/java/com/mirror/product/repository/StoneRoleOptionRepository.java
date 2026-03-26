package com.mirror.product.repository;

import com.mirror.product.entity.StoneRoleOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoneRoleOptionRepository extends JpaRepository<StoneRoleOption, Long> {
    Optional<StoneRoleOption> findByRoleCode(String code);
    List<StoneRoleOption> findByIsActiveTrueOrderByDisplayOrder();
    List<StoneRoleOption> findAllByOrderByDisplayOrder();
}
