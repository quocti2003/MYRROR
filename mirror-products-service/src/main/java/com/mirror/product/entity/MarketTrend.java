package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Market Trend - Market intelligence and trend tracking for jewelry industry
 * Migrated from mirror-mrp-service (Phase 4)
 */
@Entity
@Table(name = "market_trends")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trend_code", unique = true, nullable = false, length = 100)
    private String trendCode; // e.g., "TREND-2024-VINTAGE", "COLOR-2024-GREEN"

    @Column(name = "trend_name", nullable = false, length = 200)
    private String trendName;

    @Column(name = "trend_name_vn", length = 200)
    private String trendNameVn; // Vietnamese name

    @Column(name = "trend_type")
    @Enumerated(EnumType.STRING)
    private TrendType trendType;

    @Column(name = "trend_subtype", length = 100)
    private String trendSubtype; // e.g., for COLOR type: "gold_tone", "gem_color"

    @Column(name = "trend_period_start")
    private LocalDate trendPeriodStart;

    @Column(name = "trend_period_end")
    private LocalDate trendPeriodEnd;

    @Column(name = "impact_score")
    private Integer impactScore; // 1-100, how much this trend affects sales

    @Column(name = "confidence_level")
    @Enumerated(EnumType.STRING)
    private ConfidenceLevel confidenceLevel;

    @Column(name = "age_group_relevance", columnDefinition = "TEXT")
    private String ageGroupRelevance; // JSON object: {"18-25": 0.8, "26-35": 0.6}

    @Column(name = "geographic_relevance", columnDefinition = "TEXT")
    private String geographicRelevance; // JSON object: {"urban": 0.9, "rural": 0.3}

    @Column(name = "applicable_categories", columnDefinition = "TEXT")
    private String applicableCategories; // JSON array of jewelry category IDs

    @Column(name = "style_attributes", columnDefinition = "TEXT")
    private String styleAttributes; // JSON object with style specifications

    @Column(name = "price_impact_percentage", precision = 5, scale = 2)
    private BigDecimal priceImpactPercentage; // How much this trend affects pricing

    @Column(name = "volume_impact_percentage", precision = 5, scale = 2)
    private BigDecimal volumeImpactPercentage; // How much this trend affects demand volume

    @Column(name = "source")
    @Enumerated(EnumType.STRING)
    private TrendSource source;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "related_trends", columnDefinition = "TEXT")
    private String relatedTrends; // JSON array of related trend IDs

    @Column(name = "seasonal_pattern")
    @Enumerated(EnumType.STRING)
    private SeasonalPattern seasonalPattern;

    @Column(name = "adoption_stage")
    @Enumerated(EnumType.STRING)
    private AdoptionStage adoptionStage;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_vn", columnDefinition = "TEXT")
    private String descriptionVn; // Vietnamese description

    @Column(name = "implementation_notes", columnDefinition = "TEXT")
    private String implementationNotes; // How to implement this trend in products

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Enums

    public enum TrendType {
        STYLE, // Design style trends (vintage, modern, minimalist)
        COLOR, // Color trends (Pantone color of the year, etc.)
        MATERIAL, // Material preferences (rose gold, recycled metals)
        TECHNIQUE, // Manufacturing/finishing techniques
        SIZE, // Size preferences (delicate, statement pieces)
        OCCASION, // Occasion-based trends (work-from-home jewelry)
        CULTURAL, // Cultural influences (K-pop, Western styles)
        TECHNOLOGY, // Tech-influenced trends (smart jewelry)
        SUSTAINABILITY, // Eco-conscious trends
        ECONOMIC // Economic-driven trends (value, luxury)
    }

    public enum ConfidenceLevel {
        HIGH, // 80-100% confidence
        MEDIUM, // 60-79% confidence
        LOW, // 40-59% confidence
        EXPERIMENTAL // <40% confidence, experimental trend
    }

    public enum TrendSource {
        FASHION_FORECAST, // Professional fashion forecasting services
        SOCIAL_MEDIA, // Instagram, TikTok, etc.
        RUNWAY_SHOWS, // Fashion week, jewelry shows
        CELEBRITY, // Celebrity endorsements
        CUSTOMER_FEEDBACK, // Direct customer feedback
        SALES_DATA, // Internal sales analysis
        MARKET_RESEARCH, // Professional market research
        INDUSTRY_REPORT, // Industry publications
        TRADE_SHOW, // Jewelry trade shows
        COMPETITOR_ANALYSIS, // Competitor product analysis
        SUSTAINABILITY, // Sustainability reports
        LUXURY_REPORT, // Luxury market reports
        FASHION_REPORT // Fashion industry reports
    }

    public enum SeasonalPattern {
        SPRING, // March-May peak
        SUMMER, // June-August peak
        FALL, // September-November peak
        WINTER, // December-February peak
        HOLIDAY, // Holiday season specific
        WEDDING_SEASON, // Wedding season (varies by region)
        YEAR_ROUND, // No specific seasonal pattern
        NONE // Not seasonal
    }

    public enum AdoptionStage {
        EMERGING, // Just starting to appear
        GROWING, // Gaining momentum
        MAINSTREAM, // Widely adopted
        MATURE, // Peak adoption
        DECLINING, // Starting to decline
        FADING // Nearly obsolete
    }
}
