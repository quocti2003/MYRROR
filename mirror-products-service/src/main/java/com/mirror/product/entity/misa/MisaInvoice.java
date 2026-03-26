package com.mirror.product.entity.misa;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "misa_invoices")
@Data
@EqualsAndHashCode(callSuper = false)
public class MisaInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", unique = true, nullable = false)
    private String invoiceId;

    @Column(name = "invoice_code")
    private String invoiceCode;

    @Column(name = "ref_no")
    private String refNo;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "customer_code")
    private String customerCode;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_address", columnDefinition = "TEXT")
    private String customerAddress;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "branch_id")
    private String branchId;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "invoice_date")
    private LocalDateTime invoiceDate;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "final_amount", precision = 19, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "paid_amount", precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "debt_amount", precision = 19, scale = 2)
    private BigDecimal debtAmount;

    @Column(name = "cash_amount", precision = 19, scale = 2)
    private BigDecimal cashAmount;

    @Column(name = "card_amount", precision = 19, scale = 2)
    private BigDecimal cardAmount;

    @Column(name = "voucher_amount", precision = 19, scale = 2)
    private BigDecimal voucherAmount;

    @Column(name = "status")
    private Integer status;

    @Column(name = "status_name")
    private String statusName;

    @Column(name = "payment_status")
    private Integer paymentStatus;

    @Column(name = "payment_status_name")
    private String paymentStatusName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "misa_created_date")
    private LocalDateTime misaCreatedDate;

    @Column(name = "misa_last_modified")
    private LocalDateTime misaLastModified;

    @Column(name = "last_sync_date")
    private LocalDateTime lastSyncDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "sync_status")
    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus = SyncStatus.PENDING;

    @Column(name = "sync_error_message", columnDefinition = "TEXT")
    private String syncErrorMessage;

    public enum SyncStatus {
        PENDING,
        SYNCED,
        ERROR,
        UPDATED
    }
}
