package com.mirror.product.repository;

import com.mirror.product.entity.CountryOfOriginOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryOfOriginOptionRepository extends JpaRepository<CountryOfOriginOption, Long> {
    Optional<CountryOfOriginOption> findByCountryCode(String countryCode);
    List<CountryOfOriginOption> findByIsActiveTrueOrderByDisplayOrder();
    List<CountryOfOriginOption> findAllByOrderByDisplayOrder();
}
