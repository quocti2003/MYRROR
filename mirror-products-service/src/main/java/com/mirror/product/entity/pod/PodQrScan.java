package com.mirror.product.entity.pod;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.user.User;
import com.mirror.product.enums.DeviceType;
import com.mirror.product.enums.EntityPrefix;
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
 * Entity representing a QR code scan event.
 * Records detailed information about each scan for analytics and attribution.
 */
@Entity
@Table(name = "pod_qr_scans", indexes = {
    @Index(name = "idx_qr_scan_qr_code", columnList = "qr_code_id"),
    @Index(name = "idx_qr_scan_pod", columnList = "pod_id"),
    @Index(name = "idx_qr_scan_partner", columnList = "partner_id"),
    @Index(name = "idx_qr_scan_product", columnList = "product_id"),
    @Index(name = "idx_qr_scan_session", columnList = "session_id"),
    @Index(name = "idx_qr_scan_user", columnList = "user_id"),
    @Index(name = "idx_qr_scan_scanned_at", columnList = "scanned_at"),
    @Index(name = "idx_qr_scan_partner_date", columnList = "partner_id, scanned_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PodQrScan extends BaseEntity {

    @Column(name = "qr_code_id", nullable = false)
    private String qrCodeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_code_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PodQrCode qrCode;

    @Column(name = "pod_id", nullable = false)
    private String podId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pod_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Pod pod;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    private MirrorProduct product;

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PodPartner partner;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20)
    private DeviceType deviceType;

    @Column(name = "os", length = 50)
    private String os;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "referer", length = 500)
    private String referer;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;

    @Column(name = "is_unique", nullable = false)
    @Builder.Default
    private Boolean isUnique = true;

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PQS));
        }
        if (scannedAt == null) {
            scannedAt = Instant.now();
        }
    }
}
