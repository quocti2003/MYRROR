package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.mirror.product.util.SequenceIdGenerator;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.config.ApplicationContextProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "locations")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Location extends BaseEntity {

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private LocationType type;

    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "hours", nullable = false)
    private String hours;

    @Column(name = "phone", nullable = false, length = 50)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LocationStatus status = LocationStatus.ACTIVE;

    // MISA integration fields (for WAREHOUSE type)
    @Column(name = "misa_warehouse_id", length = 100)
    private String misaWarehouseId;

    @Column(name = "misa_warehouse_code", length = 50)
    private String misaWarehouseCode;

    @Column(name = "is_internal", nullable = false)
    private Boolean isInternal = false;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "manager_name")
    private String managerName;

    @Column(name = "manager_phone", length = 50)
    private String managerPhone;

    @Column(name = "misa_last_synced_at")
    private Instant misaLastSyncedAt;

    // Relationship: One warehouse can have many racks
    @OneToMany(mappedBy = "warehouse", fetch = FetchType.LAZY)
    private List<WarehouseRack> racks;

    // Enums
    public enum LocationType {
        SHOWROOM, BOUTIQUE, POD, WAREHOUSE
    }

    public enum LocationStatus {
        ACTIVE, INACTIVE, TEMPORARY
    }

    // Helper method to check if this is a warehouse
    public boolean isWarehouse() {
        return LocationType.WAREHOUSE.equals(this.type);
    }

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.LOC));
        }
    }

    @Override
    public String toString() {
        return "Location{" +
                "id='" + getId() + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", city='" + city + '\'' +
                ", status=" + status +
                '}';
    }
}