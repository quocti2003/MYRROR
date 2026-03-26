package com.mirror.product.dto.componentownership;

import com.mirror.product.enums.HandoffStatus;
import com.mirror.product.enums.HandoffType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for ComponentOwnershipLog entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentOwnershipLogDTO {
    private String id;

    // Relationships (IDs)
    private String productionOrderId;
    private String productionOrderNumber;
    private String stageId;
    private String stageName;
    private String fromVendorId;
    private String fromVendorName;
    private String toVendorId;
    private String toVendorName;

    // Handoff Details
    private HandoffType handoffType;
    private HandoffStatus status;

    // Initiation
    private String initiatedBy;
    private LocalDateTime initiatedAt;

    // Receipt
    private String receivedBy;
    private LocalDateTime receivedAt;

    // Transit Info
    private LocalDate expectedArrivalDate;
    private String trackingNumber;
    private String shippingCarrier;

    // Notes
    private String reason;
    private String notes;
    private String rejectionReason;

    // Computed fields
    private Boolean canConfirmReceipt;
    private Boolean canReject;
    private Boolean canCancel;
    private Boolean isTerminal;
    private Boolean isOverdue;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
