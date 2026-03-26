package com.mirror.product.controller;

import com.mirror.product.dto.WarehouseDTO.*;
import com.mirror.product.entity.*;
import com.mirror.product.entity.Location.LocationStatus;
import com.mirror.product.repository.*;
import com.mirror.product.service.WarehouseManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@Slf4j
public class WarehouseController {

    private final WarehouseManagementService warehouseService;
    private final LocationRepository locationRepository;
    private final WarehouseRackRepository rackRepository;
    private final WarehouseSlotRepository slotRepository;
    private final InventoryPositionRepository positionRepository;

    // ============ WAREHOUSE ENDPOINTS ============

    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(@RequestBody WarehouseRequest request) {
        Location warehouse = Location.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .latitude(request.getLatitude() != null ? request.getLatitude() : BigDecimal.ZERO)
                .longitude(request.getLongitude() != null ? request.getLongitude() : BigDecimal.ZERO)
                .hours(request.getHours() != null ? request.getHours() : "24/7")
                .phone(request.getPhone() != null ? request.getPhone() : "")
                .capacity(request.getCapacity())
                .managerName(request.getManagerName())
                .managerPhone(request.getManagerPhone())
                .misaWarehouseId(request.getMisaWarehouseId())
                .misaWarehouseCode(request.getMisaWarehouseCode())
                .build();

        Location created = warehouseService.createWarehouse(warehouse);
        return ResponseEntity.ok(WarehouseResponse.fromSimple(created));
    }

    @GetMapping
    public ResponseEntity<List<WarehouseResponse>> listWarehouses() {
        List<Location> warehouses = warehouseService.getActiveWarehouses();
        List<WarehouseResponse> responses = warehouses.stream()
                .map(w -> {
                    long rackCount = rackRepository.countByWarehouseId(w.getId());
                    long productCount = positionRepository.countProductsInWarehouse(w.getId());
                    return WarehouseResponse.from(w, rackCount, 0, 0, productCount);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponse> getWarehouse(@PathVariable String id) {
        return warehouseService.getWarehouseById(id)
                .map(w -> {
                    long rackCount = rackRepository.countByWarehouseId(w.getId());
                    long productCount = positionRepository.countProductsInWarehouse(w.getId());
                    long slotCount = slotRepository.countAvailableByWarehouseId(w.getId());
                    return ResponseEntity.ok(WarehouseResponse.from(w, rackCount, slotCount, 0, productCount));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponse> updateWarehouse(@PathVariable String id,
                                                             @RequestBody WarehouseRequest request) {
        Location updates = Location.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .capacity(request.getCapacity())
                .managerName(request.getManagerName())
                .managerPhone(request.getManagerPhone())
                .build();

        Location updated = warehouseService.updateWarehouse(id, updates);
        return ResponseEntity.ok(WarehouseResponse.fromSimple(updated));
    }

    // ============ RACK ENDPOINTS ============

    @PostMapping("/{warehouseId}/racks")
    public ResponseEntity<RackResponse> createRack(@PathVariable String warehouseId,
                                                   @RequestBody RackRequest request) {
        WarehouseRack rack = WarehouseRack.builder()
                .rackCode(request.getRackCode())
                .rackName(request.getRackName())
                .floorLevel(request.getFloorLevel() != null ? request.getFloorLevel() : 1)
                .rowPosition(request.getRowPosition())
                .columnPosition(request.getColumnPosition())
                .description(request.getDescription())
                .slotCapacity(request.getSlotCapacity())
                .rackType(request.getRackType() != null ?
                        WarehouseRack.RackType.valueOf(request.getRackType()) : null)
                .build();

        WarehouseRack created = warehouseService.createRack(warehouseId, rack);
        return ResponseEntity.ok(RackResponse.from(created, 0));
    }

    @GetMapping("/{warehouseId}/racks")
    public ResponseEntity<List<RackResponse>> listRacks(@PathVariable String warehouseId) {
        List<WarehouseRack> racks = warehouseService.getRacksByWarehouse(warehouseId);
        List<RackResponse> responses = racks.stream()
                .map(r -> RackResponse.from(r, slotRepository.countOccupiedByRackId(r.getId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/racks/{id}")
    public ResponseEntity<RackResponse> getRack(@PathVariable String id) {
        return warehouseService.getRackById(id)
                .map(r -> ResponseEntity.ok(RackResponse.from(r, slotRepository.countOccupiedByRackId(r.getId()))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/racks/{id}")
    public ResponseEntity<RackResponse> updateRack(@PathVariable String id, @RequestBody RackRequest request) {
        WarehouseRack updates = WarehouseRack.builder()
                .rackName(request.getRackName())
                .floorLevel(request.getFloorLevel())
                .rowPosition(request.getRowPosition())
                .columnPosition(request.getColumnPosition())
                .description(request.getDescription())
                .slotCapacity(request.getSlotCapacity())
                .rackType(request.getRackType() != null ?
                        WarehouseRack.RackType.valueOf(request.getRackType()) : null)
                .build();

        WarehouseRack updated = warehouseService.updateRack(id, updates);
        return ResponseEntity.ok(RackResponse.from(updated, slotRepository.countOccupiedByRackId(id)));
    }

    @DeleteMapping("/racks/{id}")
    public ResponseEntity<Void> deleteRack(@PathVariable String id) {
        warehouseService.deleteRack(id);
        return ResponseEntity.noContent().build();
    }

    // ============ SLOT ENDPOINTS ============

    @PostMapping("/racks/{rackId}/slots")
    public ResponseEntity<SlotResponse> createSlot(@PathVariable String rackId,
                                                   @RequestBody SlotRequest request) {
        WarehouseSlot slot = WarehouseSlot.builder()
                .slotCode(request.getSlotCode())
                .slotName(request.getSlotName())
                .slotType(request.getSlotType() != null ?
                        WarehouseSlot.SlotType.valueOf(request.getSlotType()) : null)
                .slotSize(request.getSlotSize() != null ?
                        WarehouseSlot.SlotSize.valueOf(request.getSlotSize()) : null)
                .rowInRack(request.getRowInRack())
                .columnInRack(request.getColumnInRack())
                .notes(request.getNotes())
                .build();

        WarehouseSlot created = warehouseService.createSlot(rackId, slot);
        return ResponseEntity.ok(SlotResponse.from(created));
    }

    @PostMapping("/racks/{rackId}/slots/batch")
    public ResponseEntity<List<SlotResponse>> createSlotsBatch(@PathVariable String rackId,
                                                               @RequestBody SlotBatchRequest request) {
        WarehouseSlot.SlotType slotType = request.getSlotType() != null ?
                WarehouseSlot.SlotType.valueOf(request.getSlotType()) : null;

        List<WarehouseSlot> slots = warehouseService.createSlotsInBatch(
                rackId, request.getCount(), request.getPrefix(), slotType);

        List<SlotResponse> responses = slots.stream()
                .map(SlotResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/racks/{rackId}/slots")
    public ResponseEntity<List<SlotResponse>> listSlots(@PathVariable String rackId) {
        List<WarehouseSlot> slots = warehouseService.getSlotsByRack(rackId);
        List<SlotResponse> responses = slots.stream()
                .map(SlotResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{warehouseId}/slots/available")
    public ResponseEntity<List<SlotResponse>> listAvailableSlots(@PathVariable String warehouseId) {
        List<WarehouseSlot> slots = warehouseService.getAvailableSlotsByWarehouse(warehouseId);
        List<SlotResponse> responses = slots.stream()
                .map(SlotResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/slots/{id}")
    public ResponseEntity<SlotResponse> getSlot(@PathVariable String id) {
        return warehouseService.getSlotById(id)
                .map(s -> ResponseEntity.ok(SlotResponse.from(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ============ INVENTORY POSITION ENDPOINTS ============

    @GetMapping("/positions/pending")
    public ResponseEntity<PendingPlacementSummary> getPendingPlacements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<InventoryPosition> positions = warehouseService.getPendingPlacements(page, size);
        List<PositionResponse> items = positions.getContent().stream()
                .map(PositionResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(PendingPlacementSummary.builder()
                .totalPending(positions.getTotalElements())
                .items(items)
                .build());
    }

    @GetMapping("/positions/pending/count")
    public ResponseEntity<Long> countPendingPlacements() {
        return ResponseEntity.ok(warehouseService.countPendingPlacements());
    }

    @GetMapping("/positions/product/{productCode}")
    public ResponseEntity<PositionResponse> getPositionByProduct(@PathVariable String productCode) {
        return warehouseService.getPositionByProductCode(productCode)
                .map(p -> ResponseEntity.ok(PositionResponse.from(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{warehouseId}/positions")
    public ResponseEntity<List<PositionResponse>> getPositionsByWarehouse(@PathVariable String warehouseId) {
        List<InventoryPosition> positions = warehouseService.getPositionsByWarehouse(warehouseId);
        List<PositionResponse> responses = positions.stream()
                .map(PositionResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/positions/assign")
    public ResponseEntity<PositionResponse> assignPosition(
            @RequestBody PositionAssignRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        InventoryPosition position = warehouseService.assignLocation(
                request.getProductCode(),
                request.getWarehouseId(),
                request.getRackId(),
                request.getSlotId(),
                userId != null ? userId : "system",
                request.getReason()
        );
        return ResponseEntity.ok(PositionResponse.from(position));
    }

    @PostMapping("/positions/bulk-assign")
    public ResponseEntity<List<PositionResponse>> bulkAssignPositions(
            @RequestBody PositionBulkAssignRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        List<PositionResponse> responses = request.getPositions().stream()
                .map(req -> {
                    InventoryPosition position = warehouseService.assignLocation(
                            req.getProductCode(),
                            req.getWarehouseId(),
                            req.getRackId(),
                            req.getSlotId(),
                            userId != null ? userId : "system",
                            req.getReason()
                    );
                    return PositionResponse.from(position);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // ============ STOCK INWARD WITH LOCATION ENFORCEMENT ============

    @PostMapping("/stock/inward")
    public ResponseEntity<StockInwardResponse> stockInward(
            @RequestBody StockInwardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        var result = warehouseService.processStockInward(
                request.getProductCode(),
                request.getQuantity(),
                request.getWarehouseId(),
                request.getRackId(),
                request.getSlotId(),
                userId != null ? userId : "system",
                request.getReferenceType(),
                request.getReferenceId()
        );

        return ResponseEntity.ok(StockInwardResponse.builder()
                .position(PositionResponse.from(result.position()))
                .warning(result.warning())
                .hasWarning(result.hasWarning())
                .build());
    }

    // ============ DASHBOARD STATS ============

    @GetMapping("/stats")
    public ResponseEntity<WarehouseStats> getStats() {
        long totalWarehouses = locationRepository.countActiveWarehouses();
        long pendingPlacements = warehouseService.countPendingPlacements();

        return ResponseEntity.ok(WarehouseStats.builder()
                .totalWarehouses(totalWarehouses)
                .pendingPlacements(pendingPlacements)
                .build());
    }
}
