package com.mirror.product.service;

import com.mirror.product.entity.MaterialInventory;
import com.mirror.product.entity.Vendor;
import com.mirror.product.repository.MaterialInventoryRepository;
import com.mirror.product.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Material Inventory management - Raw materials and components for jewelry production
 * Migrated from mirror-mrp-service
 */
@Service
@RequiredArgsConstructor
public class MaterialInventoryService {

    private static final Logger logger = LoggerFactory.getLogger(MaterialInventoryService.class);

    private final MaterialInventoryRepository materialInventoryRepository;
    private final VendorRepository vendorRepository;

    /**
     * Get all materials
     */
    @Transactional(readOnly = true)
    public List<MaterialInventory> findAll() {
        return materialInventoryRepository.findAll();
    }

    /**
     * Find material by ID
     */
    @Transactional(readOnly = true)
    public Optional<MaterialInventory> findById(UUID id) {
        return materialInventoryRepository.findById(id);
    }

    /**
     * Find material by name
     */
    @Transactional(readOnly = true)
    public Optional<MaterialInventory> findByName(String name) {
        return materialInventoryRepository.findByName(name);
    }

    /**
     * Find materials by type
     */
    @Transactional(readOnly = true)
    public List<MaterialInventory> findByType(String type) {
        return materialInventoryRepository.findByType(type);
    }

    /**
     * Find materials by vendor ID
     */
    @Transactional(readOnly = true)
    public List<MaterialInventory> findByVendorId(String vendorId) {
        return materialInventoryRepository.findByVendorId(vendorId);
    }

    /**
     * Find materials by vendor ID and type
     */
    @Transactional(readOnly = true)
    public List<MaterialInventory> findByVendorIdAndType(String vendorId, String type) {
        return materialInventoryRepository.findByVendorIdAndType(vendorId, type);
    }

    /**
     * Find materials below a maximum price
     */
    @Transactional(readOnly = true)
    public List<MaterialInventory> findByMaxPrice(BigDecimal maxPrice) {
        return materialInventoryRepository.findByMaxPrice(maxPrice);
    }

    /**
     * Create new material
     */
    @Transactional
    public MaterialInventory create(MaterialInventory material) {
        // Validate material name is unique
        if (material.getName() != null && materialInventoryRepository.findByName(material.getName()).isPresent()) {
            throw new IllegalArgumentException("Material with name '" + material.getName() + "' already exists");
        }

        // Validate vendor exists
        if (material.getVendor() != null && material.getVendor().getId() != null) {
            Vendor vendor = vendorRepository.findById(material.getVendor().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + material.getVendor().getId()));
            material.setVendor(vendor);
        }

        logger.info("Creating new material: {} (type: {}, vendor: {})",
                material.getName(), material.getType(),
                material.getVendor() != null ? material.getVendor().getId() : "none");

        return materialInventoryRepository.save(material);
    }

    /**
     * Update existing material
     */
    @Transactional
    public MaterialInventory update(UUID id, MaterialInventory materialDetails) {
        MaterialInventory material = materialInventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found with id: " + id));

        // Validate name uniqueness if changed
        if (materialDetails.getName() != null && !material.getName().equals(materialDetails.getName())) {
            if (materialInventoryRepository.findByName(materialDetails.getName()).isPresent()) {
                throw new IllegalArgumentException("Material with name '" + materialDetails.getName() + "' already exists");
            }
            material.setName(materialDetails.getName());
        }

        // Update fields
        if (materialDetails.getType() != null) {
            material.setType(materialDetails.getType());
        }
        if (materialDetails.getVendor() != null) {
            Vendor vendor = vendorRepository.findById(materialDetails.getVendor().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + materialDetails.getVendor().getId()));
            material.setVendor(vendor);
        }
        if (materialDetails.getBasePrice() != null) {
            material.setBasePrice(materialDetails.getBasePrice());
        }
        if (materialDetails.getMarketPrice() != null) {
            material.setMarketPrice(materialDetails.getMarketPrice());
        }
        if (materialDetails.getCurrency() != null) {
            material.setCurrency(materialDetails.getCurrency());
        }
        if (materialDetails.getTaxCustomsPercent() != null) {
            material.setTaxCustomsPercent(materialDetails.getTaxCustomsPercent());
        }
        if (materialDetails.getAssemblyCostLocal() != null) {
            material.setAssemblyCostLocal(materialDetails.getAssemblyCostLocal());
        }
        if (materialDetails.getDeliveryLeadTimeDays() != null) {
            material.setDeliveryLeadTimeDays(materialDetails.getDeliveryLeadTimeDays());
        }
        if (materialDetails.getProductionLeadTimeDays() != null) {
            material.setProductionLeadTimeDays(materialDetails.getProductionLeadTimeDays());
        }
        if (materialDetails.getAssemblyLeadTimeDays() != null) {
            material.setAssemblyLeadTimeDays(materialDetails.getAssemblyLeadTimeDays());
        }

        logger.info("Updating material: {} (ID: {})", material.getName(), id);

        return materialInventoryRepository.save(material);
    }

    /**
     * Delete material by ID
     */
    @Transactional
    public void deleteById(UUID id) {
        if (!materialInventoryRepository.existsById(id)) {
            throw new RuntimeException("Material not found with id: " + id);
        }

        // Check if material is used in any purchase orders
        MaterialInventory material = materialInventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found with id: " + id));

        if (material.getPurchaseOrderItems() != null && !material.getPurchaseOrderItems().isEmpty()) {
            throw new IllegalStateException("Cannot delete material. It is used in " +
                    material.getPurchaseOrderItems().size() + " purchase order(s)");
        }

        logger.warn("Deleting material: {} (ID: {})", material.getName(), id);

        materialInventoryRepository.deleteById(id);
    }

    /**
     * Count total materials
     */
    @Transactional(readOnly = true)
    public long count() {
        return materialInventoryRepository.count();
    }

    /**
     * Check if material exists by ID
     */
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return materialInventoryRepository.existsById(id);
    }
}
