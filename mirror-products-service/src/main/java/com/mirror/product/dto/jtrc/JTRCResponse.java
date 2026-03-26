package com.mirror.product.dto.jtrc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.JTRCStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for JTRC (Jewelry Technical Report Card)
 * Contains full detail including all components
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JTRCResponse {

    private String id;
    private String reportNumber;

    // Foreign Keys
    private String collectionPlanItemId;
    private String productId;

    // Status and Version
    private JTRCStatus status;
    private Integer version;

    // === Admin Header ===
    private String collection;
    private String season;
    private String projectId;
    private String category;
    private String source;
    private LocalDate entryDate;

    // === Pricing Snapshot ===
    private BigDecimal goldPricePerGram;
    private BigDecimal exchangeRateUsd;

    // === Calculated Cost Totals ===
    private BigDecimal totalMetalCost;
    private BigDecimal totalStoneCost;
    private BigDecimal totalLaborCost;
    private BigDecimal totalCogsVnd;
    private BigDecimal totalCogsUsd;

    // === Production Metrics ===
    private Integer productionDifficulty;
    private Integer estimatedLeadTimeDays;
    private String castingStatus;
    private String productionNotes;

    // === Visual Assets ===
    private String render3dUrl;
    private String stoneMapUrl;
    private String technicalDrawingUrl;

    // === Audit Fields ===
    private String createdBy;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isActive;

    // === Components ===
    private JTRCMetalComponentDTO metalComponent;
    private List<JTRCStoneComponentDTO> stoneComponents;
    private List<JTRCLaborComponentDTO> laborComponents;
}
