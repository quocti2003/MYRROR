package com.mirror.product.controller;

import com.mirror.product.dto.MaterialInventoryRequest;
import com.mirror.product.dto.MaterialInventoryResponse;
import com.mirror.product.entity.MaterialInventory;
import com.mirror.product.entity.Vendor;
import com.mirror.product.service.MaterialInventoryService;
import com.mirror.product.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for Material Inventory management - Raw materials and components
 * Migrated from mirror-mrp-service
 */
@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = "*")
public class MaterialInventoryController {

    @Autowired
    private MaterialInventoryService materialInventoryService;

    @Autowired
    private VendorService vendorService;

    /**
     * Get all materials
     * GET /api/materials
     */
    @GetMapping
    public ResponseEntity<List<MaterialInventoryResponse>> getAllMaterials() {
        List<MaterialInventory> materials = materialInventoryService.findAll();
        List<MaterialInventoryResponse> response = materials.stream()
                .map(MaterialInventoryResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get material by ID
     * GET /api/materials/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MaterialInventoryResponse> getMaterialById(@PathVariable UUID id) {
        return materialInventoryService.findById(id)
                .map(material -> ResponseEntity.ok(new MaterialInventoryResponse(material)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get materials by type
     * GET /api/materials/type/{type}
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<MaterialInventoryResponse>> getMaterialsByType(@PathVariable String type) {
        List<MaterialInventory> materials = materialInventoryService.findByType(type);
        List<MaterialInventoryResponse> response = materials.stream()
                .map(MaterialInventoryResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get materials by vendor
     * GET /api/materials/vendor/{vendorId}
     */
    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<MaterialInventoryResponse>> getMaterialsByVendor(@PathVariable String vendorId) {
        List<MaterialInventory> materials = materialInventoryService.findByVendorId(vendorId);
        List<MaterialInventoryResponse> response = materials.stream()
                .map(MaterialInventoryResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Create new material
     * POST /api/materials
     */
    @PostMapping
    public ResponseEntity<?> createMaterial(@Valid @RequestBody MaterialInventoryRequest request) {
        try {
            // Build material entity from request
            Vendor vendor = vendorService.findActiveById(request.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + request.getVendorId()));

            MaterialInventory material = MaterialInventory.builder()
                    .name(request.getName())
                    .type(request.getType())
                    .vendor(vendor)
                    .basePrice(request.getBasePrice())
                    .marketPrice(request.getMarketPrice())
                    .currency(request.getCurrency())
                    .taxCustomsPercent(request.getTaxCustomsPercent())
                    .assemblyCostLocal(request.getAssemblyCostLocal())
                    .deliveryLeadTimeDays(request.getDeliveryLeadTimeDays())
                    .productionLeadTimeDays(request.getProductionLeadTimeDays())
                    .assemblyLeadTimeDays(request.getAssemblyLeadTimeDays())
                    .build();

            MaterialInventory created = materialInventoryService.create(material);
            MaterialInventoryResponse response = new MaterialInventoryResponse(created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update existing material
     * PUT /api/materials/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMaterial(
            @PathVariable UUID id,
            @Valid @RequestBody MaterialInventoryRequest request) {
        try {
            // Build material entity from request
            Vendor vendor = vendorService.findActiveById(request.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + request.getVendorId()));

            MaterialInventory materialDetails = MaterialInventory.builder()
                    .name(request.getName())
                    .type(request.getType())
                    .vendor(vendor)
                    .basePrice(request.getBasePrice())
                    .marketPrice(request.getMarketPrice())
                    .currency(request.getCurrency())
                    .taxCustomsPercent(request.getTaxCustomsPercent())
                    .assemblyCostLocal(request.getAssemblyCostLocal())
                    .deliveryLeadTimeDays(request.getDeliveryLeadTimeDays())
                    .productionLeadTimeDays(request.getProductionLeadTimeDays())
                    .assemblyLeadTimeDays(request.getAssemblyLeadTimeDays())
                    .build();

            MaterialInventory updated = materialInventoryService.update(id, materialDetails);
            MaterialInventoryResponse response = new MaterialInventoryResponse(updated);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete material
     * DELETE /api/materials/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMaterial(@PathVariable UUID id) {
        try {
            materialInventoryService.deleteById(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Material deleted successfully",
                    "id", id.toString()
            ));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get material count
     * GET /api/materials/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getMaterialCount() {
        long count = materialInventoryService.count();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
