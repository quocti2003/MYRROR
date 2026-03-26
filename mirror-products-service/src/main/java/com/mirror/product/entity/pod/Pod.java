package com.mirror.product.entity.pod;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.entity.Location;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.PodStatus;
import com.mirror.product.enums.PodType;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a POD (Point of Display) unit.
 * A physical display unit placed at a partner location.
 */
@Entity
@Table(name = "pods", indexes = {
    @Index(name = "idx_pod_partner", columnList = "partner_id"),
    @Index(name = "idx_pod_status", columnList = "status"),
    @Index(name = "idx_pod_city", columnList = "city")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Pod extends BaseEntity {

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PodPartner partner;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "pod_type", nullable = false, length = 50)
    @Builder.Default
    private PodType podType = PodType.VITRINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PodStatus status = PodStatus.DRAFT;

    @Column(name = "display_capacity")
    @Builder.Default
    private Integer displayCapacity = 10;

    /**
     * Commission rate for this POD (optional).
     * If null, uses Partner's commission rate.
     */
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Reference to Location entity for map display.
     * Auto-created when POD is created.
     */
    @Column(name = "location_id")
    private String locationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Location location;

    @ManyToMany
    @JoinTable(
        name = "pod_products",
        joinColumns = @JoinColumn(name = "pod_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @Builder.Default
    private Set<MirrorProduct> products = new HashSet<>();

    @OneToMany(mappedBy = "pod", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PodQrCode> qrCodes = new ArrayList<>();

    @OneToMany(mappedBy = "pod", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PodQrScan> scans = new ArrayList<>();

    @OneToMany(mappedBy = "pod", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PodAttribution> attributions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.POD));
        }
    }

    /**
     * Get full address as a formatted string
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (locationName != null) sb.append(locationName).append(", ");
        if (addressLine1 != null) sb.append(addressLine1);
        if (addressLine2 != null) sb.append(", ").append(addressLine2);
        if (city != null) sb.append(", ").append(city);
        if (state != null) sb.append(", ").append(state);
        if (postalCode != null) sb.append(" ").append(postalCode);
        if (country != null) sb.append(", ").append(country);
        return sb.toString();
    }

    /**
     * Check if POD is operational
     */
    public boolean isOperational() {
        return status == PodStatus.ACTIVE && Boolean.TRUE.equals(getIsActive());
    }
}
