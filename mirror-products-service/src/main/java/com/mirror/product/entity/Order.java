package com.mirror.product.entity;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.OrderStatus;
import com.mirror.product.enums.PaymentStatus;
import com.mirror.product.enums.PaymentTermType;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.Builder.Default;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    private MirrorProduct product;

    @Column(name = "vendor_id")
    private String vendorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Default
    private OrderStatus status = OrderStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    @Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms_type", nullable = false, length = 50)
    @Default
    private PaymentTermType paymentTermsType = PaymentTermType.FIXED_SCHEDULE;

    @Column(name = "payment_terms", columnDefinition = "TEXT")
    private String paymentTerms;

    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration;

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Column(name = "quantity")
    @Default
    private Integer quantity = 1;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "source_channel", length = 100)
    private String sourceChannel;

    @Column(name = "payment_outstanding", precision = 15, scale = 2)
    @Default
    private BigDecimal paymentOutstanding = BigDecimal.ZERO;

    @Column(name = "subtotal_amount", precision = 15, scale = 2)
    @Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Default
    private String currency = "VND";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "expected_delivery_date")
    private Instant expectedDeliveryDate;

    @Column(name = "placed_at")
    private Instant placedAt;

    @Column(name = "last_status_updated_at")
    private Instant lastStatusUpdatedAt;

    // Workflow timestamps
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "production_started_at")
    private LocalDateTime productionStartedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Payment tracking
    @Column(name = "final_payment_received", nullable = false)
    @Default
    private Boolean finalPaymentReceived = false;

    @Column(name = "final_payment_date")
    private LocalDateTime finalPaymentDate;

    // MISA integration tracking
    @Column(name = "misa_item_created", nullable = false)
    @Default
    private Boolean misaItemCreated = false;

    @Column(name = "misa_sale_recorded", nullable = false)
    @Default
    private Boolean misaSaleRecorded = false;

    @Column(name = "misa_item_id", length = 100)
    private String misaItemId;

    @Column(name = "misa_invoice_id", length = 100)
    private String misaInvoiceId;

    // MISA retry tracking
    @Column(name = "misa_sku_retry_count", nullable = false)
    @Default
    private Integer misaSkuRetryCount = 0;

    @Column(name = "misa_invoice_retry_count", nullable = false)
    @Default
    private Integer misaInvoiceRetryCount = 0;

    @Column(name = "misa_last_sync_attempt")
    private LocalDateTime misaLastSyncAttempt;

    // Invoice forwarding tracking
    @Column(name = "invoice_forwarded", nullable = false)
    @Default
    private Boolean invoiceForwarded = false;

    @Column(name = "invoice_forwarded_date")
    private LocalDateTime invoiceForwardedDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Default
    private Set<OrderItem> items = new HashSet<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Default
    private Set<OrderPaymentSchedule> paymentSchedule = new HashSet<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Default
    private Set<OrderStatusHistory> statusHistory = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.ORD));
        }
        if (placedAt == null) {
            placedAt = Instant.now();
        }
        if (lastStatusUpdatedAt == null) {
            lastStatusUpdatedAt = placedAt;
        }
        if (paymentOutstanding == null) {
            paymentOutstanding = BigDecimal.ZERO;
        }
        if (subtotalAmount == null) {
            subtotalAmount = BigDecimal.ZERO;
        }
        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }
    }

    public void addItem(OrderItem item) {
        if (item == null) {
            return;
        }
        item.setOrder(this);
        items.add(item);
    }

    public void clearItems() {
        items.forEach(item -> item.setOrder(null));
        items.clear();
    }

    public void addPaymentScheduleEntry(OrderPaymentSchedule scheduleEntry) {
        if (scheduleEntry == null) {
            return;
        }
        scheduleEntry.setOrder(this);
        paymentSchedule.add(scheduleEntry);
    }

    public void clearPaymentSchedule() {
        paymentSchedule.forEach(entry -> entry.setOrder(null));
        paymentSchedule.clear();
    }

    public void addStatusHistory(OrderStatusHistory historyEntry) {
        if (historyEntry == null) {
            return;
        }
        historyEntry.setOrder(this);
        statusHistory.add(historyEntry);
    }
}
