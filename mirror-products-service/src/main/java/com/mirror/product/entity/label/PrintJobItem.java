package com.mirror.product.entity.label;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.PrintJobItemStatus;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Entity representing an individual item in a print job.
 * Each item corresponds to one label for one product.
 */
@Entity
@Table(name = "print_job_items", indexes = {
    @Index(name = "idx_print_job_items_job", columnList = "print_job_id"),
    @Index(name = "idx_print_job_items_product", columnList = "product_id"),
    @Index(name = "idx_print_job_items_status", columnList = "status"),
    @Index(name = "idx_print_job_items_epc", columnList = "epc")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PrintJobItem extends BaseEntity {

    @Column(name = "print_job_id", nullable = false, length = 50)
    private String printJobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "print_job_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PrintJob printJob;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    private MirrorProduct product;

    @Column(name = "epc", length = 50)
    private String epc;

    @Column(name = "zpl_data", columnDefinition = "TEXT")
    private String zplData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private PrintJobItemStatus status = PrintJobItemStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "printed_at")
    private Instant printedAt;

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PJI));
        }
    }

    /**
     * Mark as printed successfully
     */
    public void markPrinted() {
        this.status = PrintJobItemStatus.PRINTED;
        this.printedAt = Instant.now();
    }

    /**
     * Mark as failed
     */
    public void markFailed(String error) {
        this.status = PrintJobItemStatus.FAILED;
        this.errorMessage = error;
    }
}
