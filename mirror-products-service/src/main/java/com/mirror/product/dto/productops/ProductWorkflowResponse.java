package com.mirror.product.dto.productops;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductWorkflowResponse {
    private String id;
    private String name;
    private String internalSKU;
    private String misaSKU;
    private String thumbnail;
    private List<String> imageUrls;
    private String shortDescription;
    private String description;
    private String category;
    private String collection;
    private Map<String, String> specifications;
    private List<String> tags;
    private BigDecimal price;
    private String currency;

    // Workflow status
    private String status; // DRAFT, IN_PROGRESS, READY, PUBLISHED
    private Integer currentStep; // 1-7
    private List<WorkflowStepDetail> steps;

    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime markedReadyAt;

    private String markedReadyBy;

    // MISA integration
    private String misaStatus; // PENDING, SYNCING, SYNCED, FAILED

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime misaRequestedAt;

    private String misaRequestedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime misaSyncedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime misaLastAttempt;

    private Integer misaAttempts;
    private String misaError;

    // Pre-publication checklist
    private ChecklistStatus checklist;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowStepDetail {
        private Integer id; // 1-7
        private String status; // complete, pending, blocked

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
        private Instant completedAt;

        private String completedBy;
        private String notes;
        private Boolean actionButton;
        private String actionLabel;
        private String pendingMessage;
        private String blockReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistStatus {
        private Boolean hasImages;
        private Boolean hasDescription;
        private Boolean hasMISASKU;
        private Boolean hasSpecifications;
        private Boolean hasCategory;
        private Boolean hasModel3d; // iJewel Drive 3D model ID
    }
}
