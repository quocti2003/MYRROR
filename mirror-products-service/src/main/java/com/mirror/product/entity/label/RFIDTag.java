package com.mirror.product.entity.label;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.RFIDTagStatus;
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
 * Entity representing an RFID tag encoded for a product.
 * Stores EPC data and tracks scan history.
 */
@Entity
@Table(name = "rfid_tags", indexes = {
    @Index(name = "idx_rfid_tags_epc", columnList = "epc", unique = true),
    @Index(name = "idx_rfid_tags_product", columnList = "product_id"),
    @Index(name = "idx_rfid_tags_status", columnList = "status"),
    @Index(name = "idx_rfid_tags_print_job", columnList = "print_job_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RFIDTag extends BaseEntity {

    @Column(name = "epc", nullable = false, unique = true, length = 24)
    private String epc;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    private MirrorProduct product;

    @Column(name = "print_job_id", length = 50)
    private String printJobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "print_job_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    private PrintJob printJob;

    @Column(name = "print_job_item_id", length = 50)
    private String printJobItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "print_job_item_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    private PrintJobItem printJobItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private RFIDTagStatus status = RFIDTagStatus.ACTIVE;

    @Column(name = "scan_count", nullable = false)
    @Builder.Default
    private Integer scanCount = 0;

    @Column(name = "last_scanned_at")
    private Instant lastScannedAt;

    @Column(name = "last_scanned_device", length = 100)
    private String lastScannedDevice;

    @Column(name = "last_scanned_location", length = 255)
    private String lastScannedLocation;

    @Column(name = "encoded_at", nullable = false)
    private Instant encodedAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_reason", columnDefinition = "TEXT")
    private String voidedReason;

    @Column(name = "voided_by", length = 50)
    private String voidedBy;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private Map<String, Object> metadata;

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnore
    private List<RFIDScanLog> scanLogs = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.RFT));
        }
        if (encodedAt == null) {
            encodedAt = Instant.now();
        }
    }

    /**
     * Record a scan event
     */
    public void recordScan(String deviceId, String location) {
        this.scanCount++;
        this.lastScannedAt = Instant.now();
        this.lastScannedDevice = deviceId;
        this.lastScannedLocation = location;
    }

    /**
     * Void the tag
     */
    public void voidTag(String reason, String userId) {
        this.status = RFIDTagStatus.VOIDED;
        this.voidedAt = Instant.now();
        this.voidedReason = reason;
        this.voidedBy = userId;
    }

    /**
     * Check if tag is scannable
     */
    public boolean isScannable() {
        return status == RFIDTagStatus.ACTIVE && Boolean.TRUE.equals(getIsActive());
    }
}
