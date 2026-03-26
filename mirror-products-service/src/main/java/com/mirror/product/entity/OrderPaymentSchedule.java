package com.mirror.product.entity;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.PaymentScheduleStatus;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_payment_schedule")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentSchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "amount_due", nullable = false, precision = 15, scale = 2)
    @Default
    private BigDecimal amountDue = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    @Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Default
    private PaymentScheduleStatus status = PaymentScheduleStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.OPS));
        }
        if (amountDue == null) {
            amountDue = BigDecimal.ZERO;
        }
        if (amountPaid == null) {
            amountPaid = BigDecimal.ZERO;
        }
        if (status == null) {
            status = PaymentScheduleStatus.PENDING;
        }
        if (dueDate == null) {
            dueDate = Instant.now();
        }
    }
}
