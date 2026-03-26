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

/**
 * Response DTO for JTRC list view
 * Contains summary information without detailed components
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JTRCListResponse {

    private String id;
    private String reportNumber;

    // Status
    private JTRCStatus status;
    private Integer version;

    // Admin Header Summary
    private String collection;
    private String season;
    private String projectId;
    private String category;
    private LocalDate entryDate;

    // Cost Summary
    private BigDecimal totalCogsVnd;
    private BigDecimal totalCogsUsd;

    // Production Summary
    private Integer productionDifficulty;
    private Integer estimatedLeadTimeDays;

    // Component Counts (for quick overview)
    private Integer stoneComponentCount;
    private Integer laborComponentCount;
    private Boolean hasMetalComponent;

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
}
