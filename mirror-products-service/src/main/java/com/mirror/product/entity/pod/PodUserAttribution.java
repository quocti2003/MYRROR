package com.mirror.product.entity.pod;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.PodUserAttributionStatus;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "pod_user_attributions", indexes = {
    @Index(name = "idx_pua_user_pod", columnList = "user_id, pod_id"),
    @Index(name = "idx_pua_user_status", columnList = "user_id, status"),
    @Index(name = "idx_pua_partner", columnList = "partner_id"),
    @Index(name = "idx_pua_expires", columnList = "expires_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PodUserAttribution extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "pod_id", nullable = false)
    private String podId;

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "qr_code_id")
    private String qrCodeId;

    @Column(name = "first_scan_at", nullable = false)
    private Instant firstScanAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PodUserAttributionStatus status = PodUserAttributionStatus.ACTIVE;

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PUA));
        }
    }
}
