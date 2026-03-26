package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.mirror.product.util.SequenceIdGenerator;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.config.ApplicationContextProvider;
import java.util.List;

@Entity
@Table(name = "warehouse_racks", indexes = {
    @Index(name = "idx_racks_warehouse_id", columnList = "warehouse_id"),
    @Index(name = "idx_racks_rack_code", columnList = "rack_code"),
    @Index(name = "idx_racks_floor_level", columnList = "floor_level"),
    @Index(name = "idx_racks_is_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_rack_code_per_warehouse", columnNames = {"warehouse_id", "rack_code"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseRack extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Location warehouse;

    @Column(name = "rack_code", nullable = false, length = 50)
    private String rackCode;

    @Column(name = "rack_name")
    private String rackName;

    @Builder.Default
    @Column(name = "floor_level", nullable = false)
    private Integer floorLevel = 1;

    @Column(name = "row_position", length = 10)
    private String rowPosition;

    @Column(name = "column_position", length = 10)
    private String columnPosition;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "slot_capacity")
    private Integer slotCapacity;

    @Builder.Default
    @Column(name = "current_slot_count", nullable = false)
    private Integer currentSlotCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "rack_type", length = 50)
    private RackType rackType;

    // Relationship: One rack can have many slots
    @OneToMany(mappedBy = "rack", fetch = FetchType.LAZY)
    private List<WarehouseSlot> slots;

    // Enum for rack type
    public enum RackType {
        JEWELRY, DIAMOND, GENERAL, MIXED
    }

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.RAK));
        }
    }

    // Helper to get full location path
    public String getFullPath() {
        StringBuilder sb = new StringBuilder();
        if (warehouse != null) {
            sb.append(warehouse.getName());
        }
        sb.append(" > ").append(rackCode);
        if (rackName != null) {
            sb.append(" (").append(rackName).append(")");
        }
        return sb.toString();
    }

    // Check if rack has available slots
    public boolean hasAvailableSlots() {
        if (slotCapacity == null) return true;
        return currentSlotCount < slotCapacity;
    }

    @Override
    public String toString() {
        return "WarehouseRack{" +
                "id='" + getId() + '\'' +
                ", rackCode='" + rackCode + '\'' +
                ", rackName='" + rackName + '\'' +
                ", floorLevel=" + floorLevel +
                ", rackType=" + rackType +
                '}';
    }
}
