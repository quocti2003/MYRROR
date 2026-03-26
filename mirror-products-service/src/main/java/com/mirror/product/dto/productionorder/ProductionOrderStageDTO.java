package com.mirror.product.dto.productionorder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.PartnerCapabilityType;
import com.mirror.product.enums.ProductionOrderStageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Production Order Stage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductionOrderStageDTO {

    private String id;
    private String productionOrderId;
    private String workflowStageId;

    private Integer stageOrder;
    private String stageName;
    private PartnerCapabilityType requiredCapability;

    // Vendor assignment
    private String assignedVendorId;
    private String assignedVendorName;

    private ProductionOrderStageStatus status;

    // Planned dates
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;

    // Actual dates
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    // Cost tracking
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;

    private Boolean isFinalStage;
    private String notes;

    // Completion info
    private String completedBy;
    private LocalDateTime completedAt;

    // Computed fields
    private Boolean canStart;
    private Boolean canComplete;
    private Boolean canSkip;
    private Boolean canAssign;
    private Boolean isTerminal;

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
}
