package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.mirror.product.util.SequenceIdGenerator;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.config.ApplicationContextProvider;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_position_history", indexes = {
    @Index(name = "idx_pos_history_position_id", columnList = "position_id"),
    @Index(name = "idx_pos_history_product_code", columnList = "product_code"),
    @Index(name = "idx_pos_history_action_type", columnList = "action_type"),
    @Index(name = "idx_pos_history_performed_at", columnList = "performed_at"),
    @Index(name = "idx_pos_history_performed_by", columnList = "performed_by"),
    @Index(name = "idx_pos_history_to_warehouse", columnList = "to_warehouse_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryPositionHistory {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private InventoryPosition position;

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ActionType actionType;

    // Previous location (denormalized for query performance)
    @Column(name = "from_warehouse_id")
    private String fromWarehouseId;

    @Column(name = "from_warehouse_name")
    private String fromWarehouseName;

    @Column(name = "from_rack_id")
    private String fromRackId;

    @Column(name = "from_rack_code", length = 50)
    private String fromRackCode;

    @Column(name = "from_slot_id")
    private String fromSlotId;

    @Column(name = "from_slot_code", length = 50)
    private String fromSlotCode;

    // New location (denormalized for query performance)
    @Column(name = "to_warehouse_id")
    private String toWarehouseId;

    @Column(name = "to_warehouse_name")
    private String toWarehouseName;

    @Column(name = "to_rack_id")
    private String toRackId;

    @Column(name = "to_rack_code", length = 50)
    private String toRackCode;

    @Column(name = "to_slot_id")
    private String toSlotId;

    @Column(name = "to_slot_code", length = 50)
    private String toSlotCode;

    // Status change
    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30)
    private String toStatus;

    // Quantity change
    @Column(name = "quantity_before")
    private Integer quantityBefore;

    @Column(name = "quantity_after")
    private Integer quantityAfter;

    // Reference
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private String referenceId;

    // Audit
    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "performed_by_name")
    private String performedByName;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Enum for action types
    public enum ActionType {
        PLACED,         // Item placed in a location for the first time
        MOVED,          // Item moved from one location to another
        REMOVED,        // Item removed from location (sold, transferred out)
        STATUS_CHANGED, // Position status changed
        VERIFIED,       // Physical verification performed
        QUANTITY_ADJUSTED // Quantity adjusted (stock in/out)
    }

    @PrePersist
    public void prePersist() {
        if (id == null || id.isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            id = sequenceIdGenerator.generateId(EntityPrefix.IPH);
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (performedAt == null) {
            performedAt = LocalDateTime.now();
        }
    }

    // Get summary of the action
    public String getActionSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(actionType.name());

        if (actionType == ActionType.MOVED || actionType == ActionType.PLACED) {
            if (fromWarehouseName != null) {
                sb.append(" from ").append(fromWarehouseName);
                if (fromRackCode != null) sb.append("/").append(fromRackCode);
                if (fromSlotCode != null) sb.append("/").append(fromSlotCode);
            }
            if (toWarehouseName != null) {
                sb.append(" to ").append(toWarehouseName);
                if (toRackCode != null) sb.append("/").append(toRackCode);
                if (toSlotCode != null) sb.append("/").append(toSlotCode);
            }
        }

        if (quantityBefore != null && quantityAfter != null && !quantityBefore.equals(quantityAfter)) {
            sb.append(" (qty: ").append(quantityBefore).append(" → ").append(quantityAfter).append(")");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "InventoryPositionHistory{" +
                "id='" + id + '\'' +
                ", productCode='" + productCode + '\'' +
                ", actionType=" + actionType +
                ", performedAt=" + performedAt +
                ", summary='" + getActionSummary() + '\'' +
                '}';
    }
}
