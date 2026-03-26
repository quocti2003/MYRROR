package com.mirror.product.entity.label;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity for logging RFID scan events.
 * Tracks all scans including device info, location, and timestamps.
 */
@Entity
@Table(name = "rfid_scan_logs", indexes = {
    @Index(name = "idx_rfid_scan_logs_epc", columnList = "epc"),
    @Index(name = "idx_rfid_scan_logs_tag", columnList = "tag_id"),
    @Index(name = "idx_rfid_scan_logs_product", columnList = "product_id"),
    @Index(name = "idx_rfid_scan_logs_device", columnList = "device_id"),
    @Index(name = "idx_rfid_scan_logs_scanned_at", columnList = "scanned_at"),
    @Index(name = "idx_rfid_scan_logs_location", columnList = "location")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RFIDScanLog extends BaseEntity {

    @Column(name = "epc", nullable = false, length = 24)
    private String epc;

    @Column(name = "tag_id", length = 50)
    private String tagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    private RFIDTag tag;

    @Column(name = "product_id", length = 50)
    private String productId;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "is_known_tag", nullable = false)
    @Builder.Default
    private Boolean isKnownTag = false;

    @Column(name = "scan_type", length = 50)
    private String scanType;

    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.RSL));
        }
        if (scannedAt == null) {
            scannedAt = Instant.now();
        }
    }
}
