package com.mirror.product.dto;

import com.mirror.product.entity.Location;
import com.mirror.product.entity.WarehouseRack;
import com.mirror.product.entity.WarehouseSlot;
import com.mirror.product.entity.InventoryPosition;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class WarehouseDTO {

    // ============ WAREHOUSE DTOs ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseRequest {
        private String name;
        private String address;
        private String city;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String hours;
        private String phone;
        private Integer capacity;
        private String managerName;
        private String managerPhone;
        private String misaWarehouseId;
        private String misaWarehouseCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseResponse {
        private String id;
        private String name;
        private String address;
        private String city;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String hours;
        private String phone;
        private String status;
        private Integer capacity;
        private String managerName;
        private String managerPhone;
        private String misaWarehouseId;
        private String misaWarehouseCode;
        private Instant misaLastSyncedAt;
        private long rackCount;
        private long slotCount;
        private long occupiedSlotCount;
        private long productCount;
        private Instant createdAt;
        private Instant updatedAt;

        public static WarehouseResponse from(Location warehouse, long rackCount, long slotCount,
                                             long occupiedSlotCount, long productCount) {
            return WarehouseResponse.builder()
                    .id(warehouse.getId())
                    .name(warehouse.getName())
                    .address(warehouse.getAddress())
                    .city(warehouse.getCity())
                    .latitude(warehouse.getLatitude())
                    .longitude(warehouse.getLongitude())
                    .hours(warehouse.getHours())
                    .phone(warehouse.getPhone())
                    .status(warehouse.getStatus().name())
                    .capacity(warehouse.getCapacity())
                    .managerName(warehouse.getManagerName())
                    .managerPhone(warehouse.getManagerPhone())
                    .misaWarehouseId(warehouse.getMisaWarehouseId())
                    .misaWarehouseCode(warehouse.getMisaWarehouseCode())
                    .misaLastSyncedAt(warehouse.getMisaLastSyncedAt())
                    .rackCount(rackCount)
                    .slotCount(slotCount)
                    .occupiedSlotCount(occupiedSlotCount)
                    .productCount(productCount)
                    .createdAt(warehouse.getCreatedAt())
                    .updatedAt(warehouse.getUpdatedAt())
                    .build();
        }

        public static WarehouseResponse fromSimple(Location warehouse) {
            return WarehouseResponse.builder()
                    .id(warehouse.getId())
                    .name(warehouse.getName())
                    .address(warehouse.getAddress())
                    .city(warehouse.getCity())
                    .latitude(warehouse.getLatitude())
                    .longitude(warehouse.getLongitude())
                    .hours(warehouse.getHours())
                    .phone(warehouse.getPhone())
                    .status(warehouse.getStatus().name())
                    .capacity(warehouse.getCapacity())
                    .managerName(warehouse.getManagerName())
                    .managerPhone(warehouse.getManagerPhone())
                    .misaWarehouseId(warehouse.getMisaWarehouseId())
                    .misaWarehouseCode(warehouse.getMisaWarehouseCode())
                    .misaLastSyncedAt(warehouse.getMisaLastSyncedAt())
                    .createdAt(warehouse.getCreatedAt())
                    .updatedAt(warehouse.getUpdatedAt())
                    .build();
        }
    }

    // ============ RACK DTOs ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RackRequest {
        private String rackCode;
        private String rackName;
        private Integer floorLevel;
        private String rowPosition;
        private String columnPosition;
        private String description;
        private Integer slotCapacity;
        private String rackType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RackResponse {
        private String id;
        private String warehouseId;
        private String warehouseName;
        private String rackCode;
        private String rackName;
        private Integer floorLevel;
        private String rowPosition;
        private String columnPosition;
        private String description;
        private Integer slotCapacity;
        private Integer currentSlotCount;
        private String rackType;
        private Boolean isActive;
        private long occupiedSlotCount;
        private Instant createdAt;
        private Instant updatedAt;

        public static RackResponse from(WarehouseRack rack, long occupiedSlotCount) {
            return RackResponse.builder()
                    .id(rack.getId())
                    .warehouseId(rack.getWarehouse().getId())
                    .warehouseName(rack.getWarehouse().getName())
                    .rackCode(rack.getRackCode())
                    .rackName(rack.getRackName())
                    .floorLevel(rack.getFloorLevel())
                    .rowPosition(rack.getRowPosition())
                    .columnPosition(rack.getColumnPosition())
                    .description(rack.getDescription())
                    .slotCapacity(rack.getSlotCapacity())
                    .currentSlotCount(rack.getCurrentSlotCount())
                    .rackType(rack.getRackType() != null ? rack.getRackType().name() : null)
                    .isActive(rack.getIsActive())
                    .occupiedSlotCount(occupiedSlotCount)
                    .createdAt(rack.getCreatedAt())
                    .updatedAt(rack.getUpdatedAt())
                    .build();
        }
    }

    // ============ SLOT DTOs ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotRequest {
        private String slotCode;
        private String slotName;
        private String slotType;
        private String slotSize;
        private Integer rowInRack;
        private Integer columnInRack;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotBatchRequest {
        private int count;
        private String prefix;
        private String slotType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotResponse {
        private String id;
        private String rackId;
        private String rackCode;
        private String warehouseId;
        private String warehouseName;
        private String slotCode;
        private String slotName;
        private String slotType;
        private String slotSize;
        private Integer rowInRack;
        private Integer columnInRack;
        private Boolean isOccupied;
        private String currentProductCode;
        private Instant occupiedAt;
        private String occupiedBy;
        private String notes;
        private Boolean isActive;
        private String fullPath;
        private Instant createdAt;
        private Instant updatedAt;

        public static SlotResponse from(WarehouseSlot slot) {
            return SlotResponse.builder()
                    .id(slot.getId())
                    .rackId(slot.getRack().getId())
                    .rackCode(slot.getRack().getRackCode())
                    .warehouseId(slot.getRack().getWarehouse().getId())
                    .warehouseName(slot.getRack().getWarehouse().getName())
                    .slotCode(slot.getSlotCode())
                    .slotName(slot.getSlotName())
                    .slotType(slot.getSlotType() != null ? slot.getSlotType().name() : null)
                    .slotSize(slot.getSlotSize() != null ? slot.getSlotSize().name() : null)
                    .rowInRack(slot.getRowInRack())
                    .columnInRack(slot.getColumnInRack())
                    .isOccupied(slot.getIsOccupied())
                    .currentProductCode(slot.getCurrentProductCode())
                    .occupiedAt(toInstant(slot.getOccupiedAt()))
                    .occupiedBy(slot.getOccupiedBy())
                    .notes(slot.getNotes())
                    .isActive(slot.getIsActive())
                    .fullPath(slot.getFullPath())
                    .createdAt(slot.getCreatedAt())
                    .updatedAt(slot.getUpdatedAt())
                    .build();
        }
    }

    // ============ INVENTORY POSITION DTOs ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionAssignRequest {
        private String productCode;
        private String warehouseId;
        private String rackId;
        private String slotId;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionBulkAssignRequest {
        private List<PositionAssignRequest> positions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionResponse {
        private String id;
        private String productCode;
        private String warehouseId;
        private String warehouseName;
        private String rackId;
        private String rackCode;
        private String slotId;
        private String slotCode;
        private Integer quantity;
        private String positionStatus;
        private String locationSummary;
        private Boolean needsLocationAssignment;
        private Boolean hasCompleteLocation;
        private Instant assignedAt;
        private String assignedBy;
        private Instant lastVerifiedAt;
        private String lastVerifiedBy;
        private String referenceType;
        private String referenceId;
        private String notes;
        private Instant createdAt;
        private Instant updatedAt;

        public static PositionResponse from(InventoryPosition position) {
            return PositionResponse.builder()
                    .id(position.getId())
                    .productCode(position.getProductCode())
                    .warehouseId(position.getWarehouse() != null ? position.getWarehouse().getId() : null)
                    .warehouseName(position.getWarehouse() != null ? position.getWarehouse().getName() : null)
                    .rackId(position.getRack() != null ? position.getRack().getId() : null)
                    .rackCode(position.getRack() != null ? position.getRack().getRackCode() : null)
                    .slotId(position.getSlot() != null ? position.getSlot().getId() : null)
                    .slotCode(position.getSlot() != null ? position.getSlot().getSlotCode() : null)
                    .quantity(position.getQuantity())
                    .positionStatus(position.getPositionStatus().name())
                    .locationSummary(position.getLocationSummary())
                    .needsLocationAssignment(position.needsLocationAssignment())
                    .hasCompleteLocation(position.hasCompleteLocation())
                    .assignedAt(toInstant(position.getAssignedAt()))
                    .assignedBy(position.getAssignedBy())
                    .lastVerifiedAt(toInstant(position.getLastVerifiedAt()))
                    .lastVerifiedBy(position.getLastVerifiedBy())
                    .referenceType(position.getReferenceType())
                    .referenceId(position.getReferenceId())
                    .notes(position.getNotes())
                    .createdAt(position.getCreatedAt())
                    .updatedAt(position.getUpdatedAt())
                    .build();
        }
    }

    // ============ STOCK INWARD DTOs ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockInwardRequest {
        private String productCode;
        private int quantity;
        private String warehouseId;
        private String rackId;
        private String slotId;
        private String referenceType;
        private String referenceId;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockInwardResponse {
        private PositionResponse position;
        private String warning;
        private boolean hasWarning;
    }

    // ============ DASHBOARD DTOs ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingPlacementSummary {
        private long totalPending;
        private List<PositionResponse> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseStats {
        private long totalWarehouses;
        private long totalRacks;
        private long totalSlots;
        private long occupiedSlots;
        private long availableSlots;
        private long pendingPlacements;
    }

    // Helper method to convert LocalDateTime to Instant
    private static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
