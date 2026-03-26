package com.mirror.product.service;

import com.mirror.product.dto.partnercapability.PartnerCapabilityCreateRequest;
import com.mirror.product.dto.partnercapability.PartnerCapabilityDTO;
import com.mirror.product.dto.partnercapability.PartnerCapabilityUpdateRequest;
import com.mirror.product.dto.partnercapability.VendorWithCapabilityResponse;
import com.mirror.product.entity.PartnerCapability;
import com.mirror.product.entity.Vendor;
import com.mirror.product.enums.PartnerCapabilityType;
import com.mirror.product.mapper.PartnerCapabilityMapper;
import com.mirror.product.repository.PartnerCapabilityRepository;
import com.mirror.product.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing Partner Capability operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerCapabilityService {

    private final PartnerCapabilityRepository capabilityRepository;
    private final VendorRepository vendorRepository;
    private final PartnerCapabilityMapper capabilityMapper;

    /**
     * Find capability by ID
     */
    @Transactional(readOnly = true)
    public Optional<PartnerCapability> findById(String id) {
        return capabilityRepository.findActiveById(id);
    }

    /**
     * Find all capabilities for a vendor
     */
    @Transactional(readOnly = true)
    public List<PartnerCapability> findByVendorId(String vendorId) {
        return capabilityRepository.findActiveByVendorId(vendorId);
    }

    /**
     * Find capabilities by type (across all vendors)
     */
    @Transactional(readOnly = true)
    public List<PartnerCapability> findByCapabilityType(PartnerCapabilityType capabilityType) {
        return capabilityRepository.findActiveByCapabilityType(capabilityType);
    }

    /**
     * Find capabilities by type with vendor info, ordered by quality
     */
    @Transactional(readOnly = true)
    public List<PartnerCapability> findByCapabilityTypeOrderedByQuality(PartnerCapabilityType capabilityType) {
        return capabilityRepository.findActiveByCapabilityTypeWithVendorOrderedByQuality(capabilityType);
    }

    /**
     * Find vendors that have a specific capability
     */
    @Transactional(readOnly = true)
    public List<Vendor> findVendorsByCapabilityType(PartnerCapabilityType capabilityType) {
        return capabilityRepository.findVendorsByCapabilityType(capabilityType);
    }

    /**
     * Check if a vendor has a specific capability
     */
    @Transactional(readOnly = true)
    public boolean vendorHasCapability(String vendorId, PartnerCapabilityType capabilityType) {
        return capabilityRepository.existsActiveByVendorIdAndCapabilityType(vendorId, capabilityType);
    }

    /**
     * Count capabilities for a vendor
     */
    @Transactional(readOnly = true)
    public long countByVendorId(String vendorId) {
        return capabilityRepository.countActiveByVendorId(vendorId);
    }

    /**
     * Create a new capability for a vendor
     */
    @Transactional
    public PartnerCapability create(String vendorId, PartnerCapabilityCreateRequest request) {
        log.info("Creating capability for vendor {}: type={}", vendorId, request.getCapabilityType());

        // Validate vendor exists
        Vendor vendor = vendorRepository.findActiveById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + vendorId));

        // Check for duplicate capability
        if (capabilityRepository.existsActiveByVendorIdAndCapabilityType(vendorId, request.getCapabilityType())) {
            throw new IllegalArgumentException(
                    "Vendor already has capability: " + request.getCapabilityType());
        }

        // Create and save
        PartnerCapability capability = capabilityMapper.toEntity(request, vendor);
        capability = capabilityRepository.save(capability);

        log.info("Created capability with ID: {} for vendor {}", capability.getId(), vendorId);
        return capability;
    }

    /**
     * Update an existing capability
     */
    @Transactional
    public PartnerCapability update(String vendorId, String capabilityId, PartnerCapabilityUpdateRequest request) {
        log.info("Updating capability {} for vendor {}", capabilityId, vendorId);

        // Validate vendor exists
        if (!vendorRepository.existsActiveById(vendorId)) {
            throw new RuntimeException("Vendor not found with id: " + vendorId);
        }

        // Find and validate capability belongs to vendor
        PartnerCapability capability = capabilityRepository.findActiveById(capabilityId)
                .orElseThrow(() -> new RuntimeException("Capability not found with id: " + capabilityId));

        if (!capability.getVendor().getId().equals(vendorId)) {
            throw new IllegalArgumentException("Capability does not belong to vendor: " + vendorId);
        }

        // Update fields
        capabilityMapper.updateEntity(capability, request);
        capability = capabilityRepository.save(capability);

        log.info("Updated capability {} for vendor {}", capabilityId, vendorId);
        return capability;
    }

    /**
     * Soft delete a capability
     */
    @Transactional
    public void softDelete(String vendorId, String capabilityId) {
        log.info("Soft deleting capability {} for vendor {}", capabilityId, vendorId);

        // Validate vendor exists
        if (!vendorRepository.existsActiveById(vendorId)) {
            throw new RuntimeException("Vendor not found with id: " + vendorId);
        }

        // Find and validate capability belongs to vendor
        PartnerCapability capability = capabilityRepository.findActiveById(capabilityId)
                .orElseThrow(() -> new RuntimeException("Capability not found with id: " + capabilityId));

        if (!capability.getVendor().getId().equals(vendorId)) {
            throw new IllegalArgumentException("Capability does not belong to vendor: " + vendorId);
        }

        capability.setIsDeleted(true);
        capabilityRepository.save(capability);

        log.info("Soft deleted capability {} for vendor {}", capabilityId, vendorId);
    }

    /**
     * Get capability types available for a vendor (not yet assigned)
     */
    @Transactional(readOnly = true)
    public List<PartnerCapabilityType> getAvailableCapabilityTypes(String vendorId) {
        List<PartnerCapability> existingCapabilities = capabilityRepository.findActiveByVendorId(vendorId);
        List<PartnerCapabilityType> existingTypes = existingCapabilities.stream()
                .map(PartnerCapability::getCapabilityType)
                .toList();

        return java.util.Arrays.stream(PartnerCapabilityType.values())
                .filter(type -> !existingTypes.contains(type))
                .toList();
    }

    /**
     * Convert entity to DTO
     */
    public PartnerCapabilityDTO toDTO(PartnerCapability capability) {
        return capabilityMapper.toDTO(capability);
    }

    /**
     * Convert list of entities to DTOs
     */
    public List<PartnerCapabilityDTO> toDTOList(List<PartnerCapability> capabilities) {
        return capabilityMapper.toDTOList(capabilities);
    }

    /**
     * Get vendors by capability type with full capability details
     * Used for P3-02: Capability-based Partner Filter
     */
    @Transactional(readOnly = true)
    public List<VendorWithCapabilityResponse> getVendorsByCapabilityType(PartnerCapabilityType capabilityType) {
        log.info("Finding vendors with capability: {}", capabilityType);
        List<PartnerCapability> capabilities = capabilityRepository.findActiveByCapabilityTypeWithVendorOrderedByQuality(capabilityType);
        return capabilityMapper.toVendorWithCapabilityResponseList(capabilities);
    }

    /**
     * Find a specific capability for a vendor
     */
    @Transactional(readOnly = true)
    public Optional<PartnerCapability> findByVendorIdAndCapabilityType(String vendorId, PartnerCapabilityType capabilityType) {
        return capabilityRepository.findActiveByVendorIdAndCapabilityType(vendorId, capabilityType);
    }
}
