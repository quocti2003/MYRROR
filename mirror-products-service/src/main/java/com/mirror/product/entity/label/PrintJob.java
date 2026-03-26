package com.mirror.product.entity.label;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.PrintJobStatus;
import com.mirror.product.enums.PrintMethod;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a print job for label printing.
 * Tracks the overall status and contains individual print items.
 */
@Entity
@Table(name = "print_jobs", indexes = {
    @Index(name = "idx_print_jobs_status", columnList = "status"),
    @Index(name = "idx_print_jobs_template", columnList = "template_id"),
    @Index(name = "idx_print_jobs_created_at", columnList = "created_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PrintJob extends BaseEntity {

    @Column(name = "template_id", nullable = false, length = 50)
    private String templateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", referencedColumnName = "id", insertable = false, updatable = false)
    private LabelTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private PrintJobStatus status = PrintJobStatus.PENDING;

    @Column(name = "total_labels", nullable = false)
    private Integer totalLabels;

    @Column(name = "printed_labels", nullable = false)
    @Builder.Default
    private Integer printedLabels = 0;

    @Column(name = "failed_labels", nullable = false)
    @Builder.Default
    private Integer failedLabels = 0;

    @Column(name = "printer_name", length = 255)
    private String printerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "print_method", length = 50)
    private PrintMethod printMethod;

    @Column(name = "zpl_data", columnDefinition = "TEXT")
    private String zplData;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "options", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private Map<String, Object> options;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @OneToMany(mappedBy = "printJob", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PrintJobItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PJB));
        }
    }

    /**
     * Start the print job
     */
    public void start() {
        this.status = PrintJobStatus.PRINTING;
        this.startedAt = Instant.now();
    }

    /**
     * Mark job as completed
     */
    public void complete() {
        this.status = PrintJobStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Mark job as failed
     */
    public void fail(String error) {
        this.status = PrintJobStatus.FAILED;
        this.errorMessage = error;
        this.completedAt = Instant.now();
    }

    /**
     * Cancel the job
     */
    public void cancel() {
        this.status = PrintJobStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    /**
     * Increment printed count
     */
    public void incrementPrinted() {
        this.printedLabels++;
        checkCompletion();
    }

    /**
     * Increment failed count
     */
    public void incrementFailed() {
        this.failedLabels++;
        checkCompletion();
    }

    private void checkCompletion() {
        if (printedLabels + failedLabels >= totalLabels) {
            if (failedLabels > 0 && printedLabels == 0) {
                fail("All labels failed to print");
            } else {
                complete();
            }
        }
    }

    /**
     * Add an item to the job
     */
    public void addItem(PrintJobItem item) {
        items.add(item);
        item.setPrintJob(this);
    }
}
