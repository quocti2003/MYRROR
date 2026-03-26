package com.mirror.product.dto.jtrc;

import com.mirror.product.enums.JTRCStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Search criteria DTO for filtering JTRC list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JTRCSearchCriteria {

    // Text search (searches report number, collection, category, project ID)
    private String search;

    // Exact filters
    private String collection;
    private String season;
    private String category;
    private JTRCStatus status;
    private String projectId;

    // Date range filters
    private LocalDate entryDateFrom;
    private LocalDate entryDateTo;

    // Cost range filters
    private BigDecimal minCogsVnd;
    private BigDecimal maxCogsVnd;
    private BigDecimal minCogsUsd;
    private BigDecimal maxCogsUsd;

    // Production filters
    private Integer minProductionDifficulty;
    private Integer maxProductionDifficulty;
    private Integer maxLeadTimeDays;

    // Foreign key filters
    private String collectionPlanItemId;
    private String productId;
}
