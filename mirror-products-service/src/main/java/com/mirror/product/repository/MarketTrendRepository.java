package com.mirror.product.repository;

import com.mirror.product.entity.MarketTrend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for MarketTrend - Market intelligence and trend tracking for jewelry industry
 * Migrated from mirror-mrp-service (Phase 4)
 */
@Repository
public interface MarketTrendRepository extends JpaRepository<MarketTrend, Long> {

    Optional<MarketTrend> findByTrendCode(String trendCode);

    List<MarketTrend> findByTrendType(MarketTrend.TrendType trendType);

    List<MarketTrend> findByActiveTrue();

    List<MarketTrend> findByActiveTrueAndTrendPeriodStartLessThanEqualAndTrendPeriodEndGreaterThanEqual(
            LocalDate startDate, LocalDate endDate);

    List<MarketTrend> findByConfidenceLevel(MarketTrend.ConfidenceLevel confidenceLevel);

    List<MarketTrend> findByAdoptionStage(MarketTrend.AdoptionStage adoptionStage);

    List<MarketTrend> findBySource(MarketTrend.TrendSource source);
}
