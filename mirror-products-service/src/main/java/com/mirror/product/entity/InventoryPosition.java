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
import java.util.List;

@Entity
@Table(name = "inventory_positions", indexes = {
    @Index(name = "idx_positions_product_code", columnList = "product_code"),
    @Index(name = "idx_positions_warehouse_id", columnList = "warehouse_id"),
    @Index(name = "idx_positions_rack_id", columnList = "rack_id"),
    @Index(name = "idx_positions_slot_id", columnList = "slot_id"),
    @Index(name = "idx_positions_status", columnList = "position_status"),
    @Index(name = "idx_positions_is_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_position_product_code", columnNames = {"product_code"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryPosition extends BaseEntity {

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Location warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id")
    private WarehouseRack rack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private WarehouseSlot slot;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_status", nullable = false, length = 30)
    private PositionStatus positionStatus;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by")
    private String assignedBy;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "last_verified_by")
    private String lastVerifiedBy;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Relationship: One position can have many history records
    @OneToMany(mappedBy = "position", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<InventoryPositionHistory> history;

    // Enum for position status
    public enum PositionStatus {
        PLACED,             // Item is placed in a specific location (warehouse/rack/slot known)
        PENDING_PLACEMENT,  // Item has quantity but no specific location assigned
        UNASSIGNED,         // Item quantity is 0, no location needed
        IN_TRANSIT          // Item is being moved between locations
    }

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.INP));
        }
    }

    // Check if position needs location assignment
    public boolean needsLocationAssignment() {
        return quantity > 0 && warehouse == null;
    }

    // Check if position has complete location info (down to slot level)
    public boolean hasCompleteLocation() {
        return warehouse != null && rack != null && slot != null;
    }

    // Get location summary string
    public String getLocationSummary() {
        if (warehouse == null) return "Unassigned";

        StringBuilder sb = new StringBuilder(warehouse.getName());
        if (rack != null) {
            sb.append(" > ").append(rack.getRackCode());
            if (slot != null) {
                sb.append(" > ").append(slot.getSlotCode());
            }
        }
        return sb.toString();
    }

    // Assign to a location
    public void assignLocation(Location warehouse, WarehouseRack rack, WarehouseSlot slot, String userId) {
        this.warehouse = warehouse;
        this.rack = rack;
        this.slot = slot;
        this.assignedAt = LocalDateTime.now();
        this.assignedBy = userId;

        if (warehouse != null) {
            this.positionStatus = (rack != null && slot != null) ? PositionStatus.PLACED : PositionStatus.PENDING_PLACEMENT;
        } else {
            this.positionStatus = quantity > 0 ? PositionStatus.PENDING_PLACEMENT : PositionStatus.UNASSIGNED;
        }
    }

    // Update status based on quantity
    public void updateStatusByQuantity() {
        if (quantity <= 0) {
            this.positionStatus = PositionStatus.UNASSIGNED;
            // Optionally clear location when quantity becomes 0
            this.warehouse = null;
            this.rack = null;
            this.slot = null;
        } else if (warehouse == null) {
            this.positionStatus = PositionStatus.PENDING_PLACEMENT;
        } else if (slot != null) {
            this.positionStatus = PositionStatus.PLACED;
        } else {
            this.positionStatus = PositionStatus.PENDING_PLACEMENT;
        }
    }

    @Override
    public String toString() {
        return "InventoryPosition{" +
                "id='" + getId() + '\'' +
                ", productCode='" + productCode + '\'' +
                ", quantity=" + quantity +
                ", positionStatus=" + positionStatus +
                ", location='" + getLocationSummary() + '\'' +
                '}';
    }
}
