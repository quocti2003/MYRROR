package com.mirror.product.dto.productionorder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.ProductionOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for Production Order with full details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductionOrderResponse {

    private String id;
    private String orderNumber;
    private ProductionOrderStatus status;

    // Production Plan reference
    private String productionPlanId;
    private String productionPlanName;

    // Optional references
    private UUID collectionPlanItemId;
    private String collectionPlanItemName;
    private String jtrcId;
    private String jtrcReportNumber;

    private Integer quantity;

    // Current position
    private Integer currentStageOrder;
    private String currentStageName;
    private String currentHolderId;
    private String currentHolderName;

    // Dates
    private LocalDate estimatedCompletionDate;
    private LocalDate actualCompletionDate;

    private String notes;

    // P3-10: JTRC link enhancement
    private Boolean needsSpecReview;

    // Stages
    private List<ProductionOrderStageDTO> stages;

    // Progress tracking
    private Integer totalStages;
    private Integer completedStages;
    private Integer progressPercentage;

    // Computed fields
    private Boolean isEditable;
    private Boolean isCancellable;

    // Audit fields
    private String createdBy;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isActive;
}
