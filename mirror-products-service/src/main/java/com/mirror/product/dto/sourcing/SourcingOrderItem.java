package com.mirror.product.dto.sourcing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Sourcing Order Item DTO
 *
 * Represents a single production order within a sourcing report,
 * including the JTRC specifications relevant to the assigned stage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourcingOrderItem {

    private String orderId;
    private String orderNumber;
    private Integer quantity;
    private String status;

    // Stage information (the stage assigned to this vendor)
    private String stageId;
    private String stageName;
    private Integer stageOrder;
    private String requiredCapability;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private BigDecimal estimatedCost;

    // JTRC information
    private String jtrcId;
    private String reportNumber;
    private String collection;
    private String season;
    private String category;

    // JTRC Metal Component (if applicable)
    private SourcingMetalSpec metalSpec;

    // JTRC Stone Components (if applicable)
    private List<SourcingStoneSpec> stoneSpecs;

    // JTRC Labor Components (if applicable)
    private List<SourcingLaborSpec> laborSpecs;

    // Visual assets
    private String render3dUrl;
    private String stoneMapUrl;
    private String technicalDrawingUrl;

    // Production notes
    private String productionNotes;
    private Integer productionDifficulty;
    private Integer estimatedLeadTimeDays;
}
