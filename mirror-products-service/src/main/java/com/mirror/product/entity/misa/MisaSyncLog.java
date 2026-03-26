package com.mirror.product.entity.misa;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "misa_sync_logs")
@Data
@EqualsAndHashCode(callSuper = false)
public class MisaSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sync_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SyncType syncType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SyncStatus status;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "processed_records")
    private Integer processedRecords;

    @Column(name = "successful_records")
    private Integer successfulRecords;

    @Column(name = "failed_records")
    private Integer failedRecords;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "triggered_by")
    private String triggeredBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum SyncType {
        INVENTORY_ITEMS,
        CATEGORIES,
        CUSTOMERS,
        INVOICES,
        VENDORS,
        WAREHOUSES,
        FULL_SYNC,
        ORDER_SKU_CREATION,
        ORDER_INVOICE_SUBMIT,
        MISA_POLLING,
        INVENTORY_BALANCE
    }

    public enum SyncStatus {
        STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        PARTIAL_SUCCESS
    }

    public void markAsCompleted() {
        this.status = SyncStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        if (startTime != null) {
            this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }

    public void markAsFailed(String errorMessage) {
        this.status = SyncStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
        if (startTime != null) {
            this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }
}
