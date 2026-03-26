package com.mirror.product.entity;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.JTRCStatus;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Jewelry Technical Report Card (JTRC) Entity
 *
 * Core specification document for jewelry pieces containing:
 * - Admin header information (collection, season, category)
 * - Pricing snapshots (gold price, exchange rate)
 * - Metal specifications and costs
 * - Stone specifications and costs (multiple stones supported)
 * - Labor costs
 * - Visual assets (3D renders, stone maps, technical drawings)
 * - Production metrics
 */
@Entity
@Table(name = "jewelry_technical_reports", indexes = {
    @Index(name = "idx_jtrc_report_number", columnList = "report_number", unique = true),
    @Index(name = "idx_jtrc_collection", columnList = "collection"),
    @Index(name = "idx_jtrc_status", columnList = "status"),
    @Index(name = "idx_jtrc_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JewelryTechnicalReport extends BaseEntity {

    @Column(name = "report_number", nullable = false, unique = true)
    private String reportNumber; // e.g., "JTRC-2026-SPR-001"

    // Foreign Keys
    @Column(name = "collection_plan_item_id")
    private String collectionPlanItemId;

    @Column(name = "product_id")
    private String productId; // Link to MirrorProduct

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JTRCStatus status;

    @Column(nullable = false)
    private Integer version = 1; // Content version for change tracking

    // === Admin Header ===
    @Column(nullable = false)
    private String collection; // e.g., "Spring 2026"

    @Column(nullable = false)
    private String season; // e.g., "SS26", "FW26"

    @Column(name = "project_id")
    private String projectId; // Internal project reference

    @Column(nullable = false)
    private String category; // Ring, Bracelet, Necklace, Earrings, Pendant

    @Column
    private String source; // Factory Provided, Client Material, etc.

    @Column(name = "entry_date")
    private LocalDate entryDate;

    // === Pricing Snapshot ===
    @Column(name = "gold_price_per_gram", precision = 15, scale = 2)
    private BigDecimal goldPricePerGram; // Snapshot at time of entry (VND)

    @Column(name = "exchange_rate_usd", precision = 15, scale = 2)
    private BigDecimal exchangeRateUsd; // VND to USD rate at entry

    // === Calculated Cost Totals ===
    @Column(name = "total_metal_cost", precision = 15, scale = 2)
    private BigDecimal totalMetalCost;

    @Column(name = "total_stone_cost", precision = 15, scale = 2)
    private BigDecimal totalStoneCost;

    @Column(name = "total_labor_cost", precision = 15, scale = 2)
    private BigDecimal totalLaborCost;

    @Column(name = "total_cogs_vnd", precision = 15, scale = 2)
    private BigDecimal totalCogsVnd; // Total Cost of Goods Sold in VND

    @Column(name = "total_cogs_usd", precision = 15, scale = 2)
    private BigDecimal totalCogsUsd; // Total COGS in USD

    // === Production Metrics ===
    @Column(name = "production_difficulty")
    private Integer productionDifficulty; // 1-5 scale

    @Column(name = "estimated_lead_time_days")
    private Integer estimatedLeadTimeDays;

    @Column(name = "casting_status")
    private String castingStatus;

    @Column(name = "production_notes", columnDefinition = "TEXT")
    private String productionNotes;

    // === Visual Assets (S3 URLs) ===
    @Column(name = "render_3d_url")
    private String render3dUrl;

    @Column(name = "stone_map_url")
    private String stoneMapUrl;

    @Column(name = "technical_drawing_url")
    private String technicalDrawingUrl;

    // === Audit Fields ===
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // === Relationships ===
    @OneToOne(mappedBy = "jtrc", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private JTRCMetalComponent metalComponent;

    @OneToMany(mappedBy = "jtrc", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<JTRCStoneComponent> stoneComponents = new HashSet<>();

    @OneToMany(mappedBy = "jtrc", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<JTRCLaborComponent> laborComponents = new HashSet<>();

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.JTR));
        }
        if (status == null) {
            status = JTRCStatus.DRAFT;
        }
        if (version == null) {
            version = 1;
        }
    }

    @PreUpdate
    public void incrementVersion() {
        if (this.version != null) {
            this.version++;
        }
    }

    // Helper methods
    public void setMetalComponent(JTRCMetalComponent metalComponent) {
        this.metalComponent = metalComponent;
        if (metalComponent != null) {
            metalComponent.setJtrc(this);
        }
    }

    public void addStoneComponent(JTRCStoneComponent stoneComponent) {
        stoneComponents.add(stoneComponent);
        stoneComponent.setJtrc(this);
    }

    public void removeStoneComponent(JTRCStoneComponent stoneComponent) {
        stoneComponents.remove(stoneComponent);
        stoneComponent.setJtrc(null);
    }

    public void addLaborComponent(JTRCLaborComponent laborComponent) {
        laborComponents.add(laborComponent);
        laborComponent.setJtrc(this);
    }

    public void removeLaborComponent(JTRCLaborComponent laborComponent) {
        laborComponents.remove(laborComponent);
        laborComponent.setJtrc(null);
    }
}
