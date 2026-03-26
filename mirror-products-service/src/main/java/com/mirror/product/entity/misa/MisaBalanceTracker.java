package com.mirror.product.entity.misa;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "misa_balance_trackers", indexes = {
    @Index(name = "idx_balance_tracker_status", columnList = "status"),
    @Index(name = "idx_balance_tracker_item_code", columnList = "inventory_item_code"),
    @Index(name = "idx_balance_tracker_org_ref_id", columnList = "org_ref_id")
})
@Data
@EqualsAndHashCode(callSuper = false)
public class MisaBalanceTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_item_code", nullable = false)
    private String inventoryItemCode;

    @Column(name = "inventory_item_id")
    private String inventoryItemId;

    @Column(name = "stock_id")
    private String stockId;

    @Column(name = "org_ref_id")
    private String orgRefId;

    @Column(name = "org_ref_no")
    private String orgRefNo;

    @Column(name = "voucher_type")
    private Integer voucherType;

    @Column(name = "quantity_before")
    private Integer quantityBefore;

    @Column(name = "expected_quantity_after")
    private Integer expectedQuantityAfter;

    @Column(name = "quantity_change")
    private Integer quantityChange;

    @Column(name = "actual_quantity_after")
    private Integer actualQuantityAfter;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BalanceTrackerStatus status = BalanceTrackerStatus.PENDING;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "timeout_at")
    private LocalDateTime timeoutAt;

    @Column(name = "poll_attempts")
    private Integer pollAttempts = 0;

    @Column(name = "last_polled_at")
    private LocalDateTime lastPolledAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum BalanceTrackerStatus {
        PENDING,
        SUBMITTED,
        CONFIRMED,
        TIMEOUT,
        FAILED
    }
}
