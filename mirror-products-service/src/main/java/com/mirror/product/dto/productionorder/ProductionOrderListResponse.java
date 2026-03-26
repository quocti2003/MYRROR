package com.mirror.product.dto.productionorder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.ProductionOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for Production Order list view (lightweight)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductionOrderListResponse {

    private String id;
    private String orderNumber;
    private ProductionOrderStatus status;

    // Production Plan reference
    private String productionPlanId;
    private String productionPlanName;

    // Optional references
    private UUID collectionPlanItemId;
    private String jtrcId;

    private Integer quantity;

    // Current position
    private Integer currentStageOrder;
    private String currentStageName;
    private String currentHolderId;
    private String currentHolderName;

    // Dates
    private LocalDate estimatedCompletionDate;
    private LocalDate actualCompletionDate;

    // P3-10: JTRC link enhancement
    private Boolean needsSpecReview;

    // Progress tracking
    private Integer totalStages;
    private Integer completedStages;
    private Integer progressPercentage;

    // Audit fields
    private Instant createdAt;
    private Instant updatedAt;
}
