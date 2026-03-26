package com.mirror.product.repository;

import com.mirror.product.entity.StoneOriginOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoneOriginOptionRepository extends JpaRepository<StoneOriginOption, Long> {
    Optional<StoneOriginOption> findByOriginCode(String originCode);
    List<StoneOriginOption> findByIsActiveTrueOrderByDisplayOrder();
    List<StoneOriginOption> findAllByOrderByDisplayOrder();
}
