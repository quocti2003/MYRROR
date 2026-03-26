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
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_slots", indexes = {
    @Index(name = "idx_slots_rack_id", columnList = "rack_id"),
    @Index(name = "idx_slots_slot_code", columnList = "slot_code"),
    @Index(name = "idx_slots_is_occupied", columnList = "is_occupied"),
    @Index(name = "idx_slots_current_product", columnList = "current_product_code"),
    @Index(name = "idx_slots_slot_type", columnList = "slot_type"),
    @Index(name = "idx_slots_is_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_slot_code_per_rack", columnNames = {"rack_id", "slot_code"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseSlot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id", nullable = false)
    private WarehouseRack rack;

    @Column(name = "slot_code", nullable = false, length = 50)
    private String slotCode;

    @Column(name = "slot_name")
    private String slotName;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type", length = 50)
    private SlotType slotType;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_size", length = 20)
    private SlotSize slotSize;

    @Column(name = "row_in_rack")
    private Integer rowInRack;

    @Column(name = "column_in_rack")
    private Integer columnInRack;

    @Column(name = "is_occupied", nullable = false)
    private Boolean isOccupied = false;

    @Column(name = "current_product_code", length = 30)
    private String currentProductCode;

    @Column(name = "occupied_at")
    private LocalDateTime occupiedAt;

    @Column(name = "occupied_by")
    private String occupiedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Enums
    public enum SlotType {
        JEWELRY, DIAMOND, RING, NECKLACE, BRACELET, EARRING, WATCH, GENERAL
    }

    public enum SlotSize {
        SMALL, MEDIUM, LARGE
    }

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.SLT));
        }
    }

    // Helper to get full location path
    public String getFullPath() {
        StringBuilder sb = new StringBuilder();
        if (rack != null) {
            if (rack.getWarehouse() != null) {
                sb.append(rack.getWarehouse().getName()).append(" > ");
            }
            sb.append(rack.getRackCode()).append(" > ");
        }
        sb.append(slotCode);
        return sb.toString();
    }

    // Place an item in this slot
    public void placeItem(String productCode, String userId) {
        this.currentProductCode = productCode;
        this.isOccupied = true;
        this.occupiedAt = LocalDateTime.now();
        this.occupiedBy = userId;
    }

    // Remove item from this slot
    public void removeItem() {
        this.currentProductCode = null;
        this.isOccupied = false;
        this.occupiedAt = null;
        this.occupiedBy = null;
    }

    // Check if slot is available
    public boolean isAvailable() {
        return !Boolean.TRUE.equals(isOccupied) && Boolean.TRUE.equals(getIsActive());
    }

    @Override
    public String toString() {
        return "WarehouseSlot{" +
                "id='" + getId() + '\'' +
                ", slotCode='" + slotCode + '\'' +
                ", slotType=" + slotType +
                ", isOccupied=" + isOccupied +
                ", currentProductCode='" + currentProductCode + '\'' +
                '}';
    }
}
