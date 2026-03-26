package com.mirror.product.service;

import com.mirror.product.entity.*;
import com.mirror.product.entity.InventoryPosition.PositionStatus;
import com.mirror.product.entity.InventoryPositionHistory.ActionType;
import com.mirror.product.entity.Location.LocationStatus;
import com.mirror.product.entity.Location.LocationType;
import com.mirror.product.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WarehouseManagementService {

    private final LocationRepository locationRepository;
    private final WarehouseRackRepository rackRepository;
    private final WarehouseSlotRepository slotRepository;
    private final InventoryPositionRepository positionRepository;
    private final InventoryPositionHistoryRepository historyRepository;

    // ============ WAREHOUSE OPERATIONS ============

    @Transactional
    public Location createWarehouse(Location warehouse) {
        warehouse.setType(LocationType.WAREHOUSE);
        warehouse.setIsInternal(true);
        warehouse.setStatus(LocationStatus.ACTIVE);
        Location saved = locationRepository.save(warehouse);
        log.info("Created warehouse: {} ({})", saved.getName(), saved.getId());
        return saved;
    }

    public List<Location> getActiveWarehouses() {
        return locationRepository.findActiveWarehouses();
    }

    public Optional<Location> getWarehouseById(String id) {
        return locationRepository.findById(id)
                .filter(loc -> loc.getType() == LocationType.WAREHOUSE && !Boolean.TRUE.equals(loc.getIsDeleted()));
    }

    public Optional<Location> getWarehouseByMisaId(String misaWarehouseId) {
        return locationRepository.findByMisaWarehouseId(misaWarehouseId);
    }

    @Transactional
    public Location updateWarehouse(String id, Location updates) {
        Location warehouse = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + id));

        if (updates.getName() != null) warehouse.setName(updates.getName());
        if (updates.getAddress() != null) warehouse.setAddress(updates.getAddress());
        if (updates.getCity() != null) warehouse.setCity(updates.getCity());
        if (updates.getPhone() != null) warehouse.setPhone(updates.getPhone());
        if (updates.getCapacity() != null) warehouse.setCapacity(updates.getCapacity());
        if (updates.getManagerName() != null) warehouse.setManagerName(updates.getManagerName());
        if (updates.getManagerPhone() != null) warehouse.setManagerPhone(updates.getManagerPhone());
        if (updates.getStatus() != null) warehouse.setStatus(updates.getStatus());

        return locationRepository.save(warehouse);
    }

    // ============ RACK OPERATIONS ============

    @Transactional
    public WarehouseRack createRack(String warehouseId, WarehouseRack rack) {
        Location warehouse = locationRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + warehouseId));

        if (rackRepository.existsByWarehouseIdAndRackCodeAndIsDeletedFalse(warehouseId, rack.getRackCode())) {
            throw new IllegalArgumentException("Rack code already exists in this warehouse: " + rack.getRackCode());
        }

        rack.setWarehouse(warehouse);
        WarehouseRack saved = rackRepository.save(rack);
        log.info("Created rack: {} in warehouse {}", saved.getRackCode(), warehouse.getName());
        return saved;
    }

    public List<WarehouseRack> getRacksByWarehouse(String warehouseId) {
        return rackRepository.findActiveRacksByWarehouseId(warehouseId);
    }

    public Optional<WarehouseRack> getRackById(String id) {
        return rackRepository.findByIdAndIsDeletedFalse(id);
    }

    @Transactional
    public WarehouseRack updateRack(String id, WarehouseRack updates) {
        WarehouseRack rack = rackRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Rack not found: " + id));

        if (updates.getRackName() != null) rack.setRackName(updates.getRackName());
        if (updates.getFloorLevel() != null) rack.setFloorLevel(updates.getFloorLevel());
        if (updates.getRowPosition() != null) rack.setRowPosition(updates.getRowPosition());
        if (updates.getColumnPosition() != null) rack.setColumnPosition(updates.getColumnPosition());
        if (updates.getDescription() != null) rack.setDescription(updates.getDescription());
        if (updates.getSlotCapacity() != null) rack.setSlotCapacity(updates.getSlotCapacity());
        if (updates.getRackType() != null) rack.setRackType(updates.getRackType());
        if (updates.getIsActive() != null) rack.setIsActive(updates.getIsActive());

        return rackRepository.save(rack);
    }

    @Transactional
    public void deleteRack(String id) {
        WarehouseRack rack = rackRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Rack not found: " + id));

        // Check if rack has items
        long occupiedSlots = slotRepository.countOccupiedByRackId(id);
        if (occupiedSlots > 0) {
            throw new IllegalStateException("Cannot delete rack with occupied slots. Move items first.");
        }

        rack.setIsDeleted(true);
        rackRepository.save(rack);
        log.info("Deleted rack: {}", id);
    }

    // ============ SLOT OPERATIONS ============

    @Transactional
    public WarehouseSlot createSlot(String rackId, WarehouseSlot slot) {
        WarehouseRack rack = rackRepository.findByIdAndIsDeletedFalse(rackId)
                .orElseThrow(() -> new IllegalArgumentException("Rack not found: " + rackId));

        if (slotRepository.existsByRackIdAndSlotCodeAndIsDeletedFalse(rackId, slot.getSlotCode())) {
            throw new IllegalArgumentException("Slot code already exists in this rack: " + slot.getSlotCode());
        }

        slot.setRack(rack);
        slot.setIsOccupied(false);
        WarehouseSlot saved = slotRepository.save(slot);

        // Update rack slot count
        rack.setCurrentSlotCount(rack.getCurrentSlotCount() + 1);
        rackRepository.save(rack);

        log.info("Created slot: {} in rack {}", saved.getSlotCode(), rack.getRackCode());
        return saved;
    }

    @Transactional
    public List<WarehouseSlot> createSlotsInBatch(String rackId, int count, String prefix, WarehouseSlot.SlotType slotType) {
        WarehouseRack rack = rackRepository.findByIdAndIsDeletedFalse(rackId)
                .orElseThrow(() -> new IllegalArgumentException("Rack not found: " + rackId));

        long existingCount = slotRepository.countByRackId(rackId);

        for (int i = 1; i <= count; i++) {
            String slotCode = String.format("%s%02d", prefix, existingCount + i);
            WarehouseSlot slot = WarehouseSlot.builder()
                    .rack(rack)
                    .slotCode(slotCode)
                    .slotType(slotType)
                    .isOccupied(false)
                    .rowInRack((int) ((existingCount + i - 1) / 10) + 1)
                    .columnInRack((int) ((existingCount + i - 1) % 10) + 1)
                    .build();
            slotRepository.save(slot);
        }

        rack.setCurrentSlotCount((int) (existingCount + count));
        rackRepository.save(rack);

        log.info("Created {} slots in rack {}", count, rack.getRackCode());
        return slotRepository.findByRackIdAndIsDeletedFalse(rackId);
    }

    public List<WarehouseSlot> getSlotsByRack(String rackId) {
        return slotRepository.findByRackIdAndIsDeletedFalse(rackId);
    }

    public List<WarehouseSlot> getAvailableSlotsByWarehouse(String warehouseId) {
        return slotRepository.findAvailableSlotsByWarehouseId(warehouseId);
    }

    public Optional<WarehouseSlot> getSlotById(String id) {
        return slotRepository.findByIdAndIsDeletedFalse(id);
    }

    // ============ INVENTORY POSITION OPERATIONS ============

    @Transactional
    public InventoryPosition createOrUpdatePosition(String productCode, String warehouseId,
                                                     String rackId, String slotId,
                                                     int quantity, String userId, String reason) {
        InventoryPosition position = positionRepository.findByProductCodeAndIsDeletedFalse(productCode)
                .orElse(InventoryPosition.builder()
                        .productCode(productCode)
                        .quantity(0)
                        .positionStatus(PositionStatus.UNASSIGNED)
                        .build());

        // Store old values for history
        String oldWarehouseId = position.getWarehouse() != null ? position.getWarehouse().getId() : null;
        String oldRackId = position.getRack() != null ? position.getRack().getId() : null;
        String oldSlotId = position.getSlot() != null ? position.getSlot().getId() : null;
        int oldQuantity = position.getQuantity();
        PositionStatus oldStatus = position.getPositionStatus();

        // Update position
        position.setQuantity(quantity);

        Location warehouse = warehouseId != null ? locationRepository.findById(warehouseId).orElse(null) : null;
        WarehouseRack rack = rackId != null ? rackRepository.findByIdAndIsDeletedFalse(rackId).orElse(null) : null;
        WarehouseSlot slot = slotId != null ? slotRepository.findByIdAndIsDeletedFalse(slotId).orElse(null) : null;

        position.setWarehouse(warehouse);
        position.setRack(rack);
        position.setSlot(slot);

        if (slot != null) {
            // Update slot occupancy
            slot.placeItem(productCode, userId);
            slotRepository.save(slot);
        }

        position.updateStatusByQuantity();
        position.setAssignedAt(LocalDateTime.now());
        position.setAssignedBy(userId);

        InventoryPosition saved = positionRepository.save(position);

        // Record history
        recordPositionHistory(saved, oldWarehouseId, oldRackId, oldSlotId, oldQuantity, oldStatus, userId, reason);

        log.info("Updated position for {}: qty={}, location={}", productCode, quantity, saved.getLocationSummary());
        return saved;
    }

    @Transactional
    public InventoryPosition assignLocation(String productCode, String warehouseId,
                                            String rackId, String slotId, String userId, String reason) {
        InventoryPosition position = positionRepository.findByProductCodeAndIsDeletedFalse(productCode)
                .orElseThrow(() -> new IllegalArgumentException("Position not found for product: " + productCode));

        String oldWarehouseId = position.getWarehouse() != null ? position.getWarehouse().getId() : null;
        String oldRackId = position.getRack() != null ? position.getRack().getId() : null;
        String oldSlotId = position.getSlot() != null ? position.getSlot().getId() : null;
        PositionStatus oldStatus = position.getPositionStatus();

        // Clear old slot if exists
        if (position.getSlot() != null) {
            position.getSlot().removeItem();
            slotRepository.save(position.getSlot());
        }

        Location warehouse = warehouseId != null ? locationRepository.findById(warehouseId).orElse(null) : null;
        WarehouseRack rack = rackId != null ? rackRepository.findByIdAndIsDeletedFalse(rackId).orElse(null) : null;
        WarehouseSlot slot = slotId != null ? slotRepository.findByIdAndIsDeletedFalse(slotId).orElse(null) : null;

        if (slot != null && slot.getIsOccupied()) {
            throw new IllegalStateException("Slot is already occupied by: " + slot.getCurrentProductCode());
        }

        position.assignLocation(warehouse, rack, slot, userId);

        if (slot != null) {
            slot.placeItem(productCode, userId);
            slotRepository.save(slot);
        }

        InventoryPosition saved = positionRepository.save(position);

        // Record history
        recordPositionHistory(saved, oldWarehouseId, oldRackId, oldSlotId, position.getQuantity(), oldStatus, userId, reason);

        log.info("Assigned location for {}: {}", productCode, saved.getLocationSummary());
        return saved;
    }

    public Optional<InventoryPosition> getPositionByProductCode(String productCode) {
        return positionRepository.findByProductCodeAndIsDeletedFalse(productCode);
    }

    public Page<InventoryPosition> getPendingPlacements(int page, int size) {
        return positionRepository.findPendingPlacements(PageRequest.of(page, size));
    }

    public long countPendingPlacements() {
        return positionRepository.countPendingPlacements();
    }

    public List<InventoryPosition> getPositionsByWarehouse(String warehouseId) {
        return positionRepository.findByWarehouseIdWithDetails(warehouseId);
    }

    // ============ HISTORY OPERATIONS ============

    public Page<InventoryPositionHistory> getPositionHistory(String productCode, int page, int size) {
        return historyRepository.findByProductCodeOrderByPerformedAtDesc(productCode, PageRequest.of(page, size));
    }

    public Page<InventoryPositionHistory> getRecentActivity(int page, int size) {
        return historyRepository.findRecentActivity(PageRequest.of(page, size));
    }

    // ============ STOCK INWARD ENFORCEMENT ============

    /**
     * Check if product needs location assignment (soft warning)
     * Returns a warning message if location is needed, null otherwise
     */
    public String checkLocationRequirement(String productCode, int newQuantity) {
        if (newQuantity <= 0) {
            return null; // No location needed for zero quantity
        }

        Optional<InventoryPosition> positionOpt = positionRepository.findByProductCodeAndIsDeletedFalse(productCode);

        if (positionOpt.isEmpty()) {
            return "Sản phẩm chưa được gán vị trí kho. Vui lòng chọn kho lưu trữ.";
        }

        InventoryPosition position = positionOpt.get();
        if (position.getWarehouse() == null) {
            return "Sản phẩm chưa được gán kho. Vui lòng chọn kho lưu trữ.";
        }

        if (position.getSlot() == null) {
            return "Sản phẩm đã có kho nhưng chưa có vị trí chi tiết (kệ/ô). Khuyến nghị hoàn thiện thông tin.";
        }

        return null; // All location info complete
    }

    /**
     * Process stock inward with location enforcement
     */
    @Transactional
    public StockInwardResult processStockInward(String productCode, int quantity,
                                                 String warehouseId, String rackId, String slotId,
                                                 String userId, String referenceType, String referenceId) {
        // Check location requirement
        String warning = null;
        if (warehouseId == null) {
            warning = checkLocationRequirement(productCode, quantity);
        }

        // Create or update position
        InventoryPosition position = createOrUpdatePosition(
                productCode, warehouseId, rackId, slotId, quantity, userId,
                "Stock inward: " + referenceType + " " + referenceId
        );

        position.setReferenceType(referenceType);
        position.setReferenceId(referenceId);
        positionRepository.save(position);

        return new StockInwardResult(position, warning);
    }

    // Helper class for stock inward result
    public record StockInwardResult(InventoryPosition position, String warning) {
        public boolean hasWarning() {
            return warning != null && !warning.isEmpty();
        }
    }

    // ============ PRIVATE HELPERS ============

    private void recordPositionHistory(InventoryPosition position,
                                       String oldWarehouseId, String oldRackId, String oldSlotId,
                                       int oldQuantity, PositionStatus oldStatus,
                                       String userId, String reason) {
        ActionType actionType;
        if (oldWarehouseId == null && position.getWarehouse() != null) {
            actionType = ActionType.PLACED;
        } else if (oldWarehouseId != null && position.getWarehouse() == null) {
            actionType = ActionType.REMOVED;
        } else if (!java.util.Objects.equals(oldWarehouseId, position.getWarehouse() != null ? position.getWarehouse().getId() : null) ||
                   !java.util.Objects.equals(oldRackId, position.getRack() != null ? position.getRack().getId() : null) ||
                   !java.util.Objects.equals(oldSlotId, position.getSlot() != null ? position.getSlot().getId() : null)) {
            actionType = ActionType.MOVED;
        } else if (oldQuantity != position.getQuantity()) {
            actionType = ActionType.QUANTITY_ADJUSTED;
        } else if (oldStatus != position.getPositionStatus()) {
            actionType = ActionType.STATUS_CHANGED;
        } else {
            return; // No change to record
        }

        InventoryPositionHistory history = InventoryPositionHistory.builder()
                .position(position)
                .productCode(position.getProductCode())
                .actionType(actionType)
                .fromWarehouseId(oldWarehouseId)
                .fromWarehouseName(oldWarehouseId != null ?
                        locationRepository.findById(oldWarehouseId).map(Location::getName).orElse(null) : null)
                .fromRackId(oldRackId)
                .fromRackCode(oldRackId != null ?
                        rackRepository.findById(oldRackId).map(WarehouseRack::getRackCode).orElse(null) : null)
                .fromSlotId(oldSlotId)
                .fromSlotCode(oldSlotId != null ?
                        slotRepository.findById(oldSlotId).map(WarehouseSlot::getSlotCode).orElse(null) : null)
                .toWarehouseId(position.getWarehouse() != null ? position.getWarehouse().getId() : null)
                .toWarehouseName(position.getWarehouse() != null ? position.getWarehouse().getName() : null)
                .toRackId(position.getRack() != null ? position.getRack().getId() : null)
                .toRackCode(position.getRack() != null ? position.getRack().getRackCode() : null)
                .toSlotId(position.getSlot() != null ? position.getSlot().getId() : null)
                .toSlotCode(position.getSlot() != null ? position.getSlot().getSlotCode() : null)
                .fromStatus(oldStatus != null ? oldStatus.name() : null)
                .toStatus(position.getPositionStatus().name())
                .quantityBefore(oldQuantity)
                .quantityAfter(position.getQuantity())
                .performedBy(userId)
                .performedAt(LocalDateTime.now())
                .reason(reason)
                .build();

        historyRepository.save(history);
    }
}
