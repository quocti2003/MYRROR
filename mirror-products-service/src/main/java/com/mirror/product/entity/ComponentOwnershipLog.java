package com.mirror.product.entity;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.HandoffStatus;
import com.mirror.product.enums.HandoffType;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ComponentOwnershipLog Entity
 *
 * Maintains an immutable audit trail of component ownership transfers throughout
 * the production workflow. Every time a component changes hands (vendor to vendor,
 * vendor to MIRROR, etc.), a new log entry is created.
 *
 * This enables:
 * - Full traceability of component location history
 * - Audit compliance for production chain
 * - Dispute resolution (who had what, when)
 * - Transit time analysis
 * - Vendor performance metrics
 */
@Entity
@Table(name = "component_ownership_logs", indexes = {
    @Index(name = "idx_col_production_order", columnList = "production_order_id"),
    @Index(name = "idx_col_stage", columnList = "stage_id"),
    @Index(name = "idx_col_from_vendor", columnList = "from_vendor_id"),
    @Index(name = "idx_col_to_vendor", columnList = "to_vendor_id"),
    @Index(name = "idx_col_status", columnList = "status"),
    @Index(name = "idx_col_handoff_type", columnList = "handoff_type"),
    @Index(name = "idx_col_initiated_at", columnList = "initiated_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ComponentOwnershipLog extends BaseEntity {

    /**
     * The production order being transferred
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    /**
     * The stage associated with this handoff (nullable for non-stage transfers)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private ProductionOrderStage stage;

    /**
     * The vendor handing off the component (null for INITIAL_ASSIGNMENT from MIRROR)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_vendor_id")
    private Vendor fromVendor;

    /**
     * The vendor receiving the component (null for RETURN_TO_MIRROR)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_vendor_id")
    private Vendor toVendor;

    /**
     * Type of handoff event
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "handoff_type", nullable = false)
    private HandoffType handoffType;

    /**
     * Current status of the handoff
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private HandoffStatus status = HandoffStatus.INITIATED;

    /**
     * User who initiated the handoff
     */
    @Column(name = "initiated_by", nullable = false)
    private String initiatedBy;

    /**
     * Timestamp when handoff was initiated
     */
    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    /**
     * User who confirmed receipt (vendor user)
     */
    @Column(name = "received_by")
    private String receivedBy;

    /**
     * Timestamp when receipt was confirmed
     */
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    /**
     * Expected arrival date for transit planning
     */
    @Column(name = "expected_arrival_date")
    private LocalDate expectedArrivalDate;

    /**
     * Reason for this handoff (e.g., stage completion, vendor reassignment)
     */
    @Column(name = "reason")
    private String reason;

    /**
     * Additional notes about the handoff
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Rejection reason if status is REJECTED
     */
    @Column(name = "rejection_reason")
    private String rejectionReason;

    /**
     * Tracking number for physical shipment (if applicable)
     */
    @Column(name = "tracking_number")
    private String trackingNumber;

    /**
     * Shipping carrier (if applicable)
     */
    @Column(name = "shipping_carrier")
    private String shippingCarrier;

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.HOL));
        }
        if (status == null) {
            status = HandoffStatus.INITIATED;
        }
        if (initiatedAt == null) {
            initiatedAt = LocalDateTime.now();
        }
    }

    /**
     * Check if handoff can be confirmed as received
     */
    public boolean canConfirmReceipt() {
        return status == HandoffStatus.INITIATED || status == HandoffStatus.IN_TRANSIT;
    }

    /**
     * Check if handoff can be rejected
     */
    public boolean canReject() {
        return status == HandoffStatus.INITIATED || status == HandoffStatus.IN_TRANSIT;
    }

    /**
     * Check if handoff can be cancelled
     */
    public boolean canCancel() {
        return status == HandoffStatus.INITIATED;
    }

    /**
     * Check if handoff is in a terminal state
     */
    public boolean isTerminal() {
        return status == HandoffStatus.RECEIVED ||
               status == HandoffStatus.REJECTED ||
               status == HandoffStatus.CANCELLED;
    }

    /**
     * Mark handoff as in transit
     */
    public void markInTransit(String trackingNumber, String carrier) {
        this.status = HandoffStatus.IN_TRANSIT;
        this.trackingNumber = trackingNumber;
        this.shippingCarrier = carrier;
    }

    /**
     * Confirm receipt of component
     */
    public void confirmReceipt(String userId) {
        this.status = HandoffStatus.RECEIVED;
        this.receivedBy = userId;
        this.receivedAt = LocalDateTime.now();
    }

    /**
     * Reject the handoff
     */
    public void reject(String userId, String reason) {
        this.status = HandoffStatus.REJECTED;
        this.receivedBy = userId;
        this.receivedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }
}
