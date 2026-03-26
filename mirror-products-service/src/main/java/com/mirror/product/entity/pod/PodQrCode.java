package com.mirror.product.entity.pod;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.QrCodeType;
import com.mirror.product.enums.QrCodeStatus;
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
 * Entity representing a QR Code for a product in a POD.
 * Each QR code links to a specific product and tracks scans.
 */
@Entity
@Table(name = "pod_qr_codes", indexes = {
    @Index(name = "idx_qr_code_short_code", columnList = "short_code"),
    @Index(name = "idx_qr_code_pod", columnList = "pod_id"),
    @Index(name = "idx_qr_code_product", columnList = "product_id"),
    @Index(name = "idx_qr_code_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PodQrCode extends BaseEntity {

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

    @Column(name = "short_code", nullable = false, unique = true, length = 20)
    private String shortCode;

    @Column(name = "full_url", length = 500)
    private String fullUrl;

    @Column(name = "qr_image_url", length = 500)
    private String qrImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "qr_code_type", nullable = false, length = 20)
    @Builder.Default
    private QrCodeType qrCodeType = QrCodeType.POD_QR;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private QrCodeStatus status = QrCodeStatus.ACTIVE;

    @Column(name = "scan_count", nullable = false)
    @Builder.Default
    private Long scanCount = 0L;

    @Column(name = "last_scanned_at")
    private Instant lastScannedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @OneToMany(mappedBy = "qrCode", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PodQrScan> scans = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PQR));
        }
    }

    /**
     * Increment scan count and update last scanned timestamp
     */
    public void recordScan() {
        this.scanCount++;
        this.lastScannedAt = Instant.now();
    }

    /**
     * Check if QR code is scannable
     */
    public boolean isScannable() {
        if (status != QrCodeStatus.ACTIVE) {
            return false;
        }
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return false;
        }
        return Boolean.TRUE.equals(getIsActive());
    }
}
