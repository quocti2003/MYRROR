package com.mirror.product.controller;

import com.mirror.product.dto.partnercapability.PartnerCapabilityCreateRequest;
import com.mirror.product.dto.partnercapability.PartnerCapabilityDTO;
import com.mirror.product.dto.partnercapability.PartnerCapabilityUpdateRequest;
import com.mirror.product.dto.partnercapability.VendorWithCapabilityResponse;
import com.mirror.product.entity.PartnerCapability;
import com.mirror.product.entity.Vendor;
import com.mirror.product.enums.PartnerCapabilityType;
import com.mirror.product.service.PartnerCapabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Partner Capability operations
 * Handles vendor-specific capabilities and capability-based vendor search
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class PartnerCapabilityController {

    private final PartnerCapabilityService capabilityService;

    // ==================== Vendor-specific Capability Endpoints ====================

    /**
     * Get all capabilities for a vendor
     * GET /api/v1/vendors/{vendorId}/capabilities
     */
    @GetMapping("/vendors/{vendorId}/capabilities")
    public ResponseEntity<?> getVendorCapabilities(@PathVariable String vendorId) {
        try {
            List<PartnerCapability> capabilities = capabilityService.findByVendorId(vendorId);
            List<PartnerCapabilityDTO> responses = capabilityService.toDTOList(capabilities);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting capabilities for vendor: {}", vendorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching vendor capabilities");
        }
    }

    /**
     * Get a specific capability for a vendor
     * GET /api/v1/vendors/{vendorId}/capabilities/{capabilityId}
     */
    @GetMapping("/vendors/{vendorId}/capabilities/{capabilityId}")
    public ResponseEntity<?> getVendorCapability(
            @PathVariable String vendorId,
            @PathVariable String capabilityId) {
        try {
            return capabilityService.findById(capabilityId)
                    .filter(c -> c.getVendor().getId().equals(vendorId))
                    .map(capabilityService::toDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting capability {} for vendor: {}", capabilityId, vendorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching vendor capability");
        }
    }

    /**
     * Create a new capability for a vendor
     * POST /api/v1/vendors/{vendorId}/capabilities
     */
    @PostMapping("/vendors/{vendorId}/capabilities")
    public ResponseEntity<?> createVendorCapability(
            @PathVariable String vendorId,
            @Valid @RequestBody PartnerCapabilityCreateRequest request) {
        try {
            PartnerCapability capability = capabilityService.create(vendorId, request);
            PartnerCapabilityDTO response = capabilityService.toDTO(capability);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for creating capability: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating capability for vendor: {}", vendorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating vendor capability");
        }
    }

    /**
     * Update a capability for a vendor
     * PUT /api/v1/vendors/{vendorId}/capabilities/{capabilityId}
     */
    @PutMapping("/vendors/{vendorId}/capabilities/{capabilityId}")
    public ResponseEntity<?> updateVendorCapability(
            @PathVariable String vendorId,
            @PathVariable String capabilityId,
            @Valid @RequestBody PartnerCapabilityUpdateRequest request) {
        try {
            PartnerCapability capability = capabilityService.update(vendorId, capabilityId, request);
            PartnerCapabilityDTO response = capabilityService.toDTO(capability);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for updating capability: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating capability {} for vendor: {}", capabilityId, vendorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating vendor capability");
        }
    }

    /**
     * Delete a capability for a vendor
     * DELETE /api/v1/vendors/{vendorId}/capabilities/{capabilityId}
     */
    @DeleteMapping("/vendors/{vendorId}/capabilities/{capabilityId}")
    public ResponseEntity<?> deleteVendorCapability(
            @PathVariable String vendorId,
            @PathVariable String capabilityId) {
        try {
            capabilityService.softDelete(vendorId, capabilityId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Cannot delete capability: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting capability {} for vendor: {}", capabilityId, vendorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while deleting vendor capability");
        }
    }

    /**
     * Get available capability types for a vendor (not yet assigned)
     * GET /api/v1/vendors/{vendorId}/capabilities/available-types
     */
    @GetMapping("/vendors/{vendorId}/capabilities/available-types")
    public ResponseEntity<?> getAvailableCapabilityTypes(@PathVariable String vendorId) {
        try {
            List<PartnerCapabilityType> availableTypes = capabilityService.getAvailableCapabilityTypes(vendorId);
            return ResponseEntity.ok(availableTypes);
        } catch (Exception e) {
            log.error("Error getting available capability types for vendor: {}", vendorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching available capability types");
        }
    }

    /**
     * Check if vendor has a specific capability
     * GET /api/v1/vendors/{vendorId}/capabilities/has/{capabilityType}
     */
    @GetMapping("/vendors/{vendorId}/capabilities/has/{capabilityType}")
    public ResponseEntity<?> checkVendorHasCapability(
            @PathVariable String vendorId,
            @PathVariable PartnerCapabilityType capabilityType) {
        try {
            boolean hasCapability = capabilityService.vendorHasCapability(vendorId, capabilityType);
            return ResponseEntity.ok(Map.of("hasCapability", hasCapability));
        } catch (Exception e) {
            log.error("Error checking capability {} for vendor: {}", capabilityType, vendorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while checking vendor capability");
        }
    }

    // ==================== Capability-based Search Endpoints ====================

    /**
     * Find vendors by capability type
     * GET /api/v1/capabilities/by-type/{type}
     */
    @GetMapping("/capabilities/by-type/{type}")
    public ResponseEntity<?> getVendorsByCapabilityType(@PathVariable PartnerCapabilityType type) {
        try {
            List<PartnerCapability> capabilities = capabilityService.findByCapabilityTypeOrderedByQuality(type);
            List<PartnerCapabilityDTO> responses = capabilityService.toDTOList(capabilities);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting vendors by capability type: {}", type, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching vendors by capability");
        }
    }

    /**
     * P3-02: Capability-based Partner Filter
     * Get vendors (partners) with specific capability, ordered by quality rating
     * Returns full vendor and capability details for partner selection
     * GET /api/v1/partners/by-capability/{capabilityType}
     */
    @GetMapping("/partners/by-capability/{capabilityType}")
    public ResponseEntity<?> getPartnersByCapability(@PathVariable PartnerCapabilityType capabilityType) {
        try {
            log.info("Finding partners with capability: {}", capabilityType);
            List<VendorWithCapabilityResponse> responses = capabilityService.getVendorsByCapabilityType(capabilityType);
            return ResponseEntity.ok(Map.of(
                    "capabilityType", capabilityType,
                    "totalPartners", responses.size(),
                    "partners", responses
            ));
        } catch (Exception e) {
            log.error("Error getting partners by capability: {}", capabilityType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching partners by capability");
        }
    }

    /**
     * Get all capability types
     * GET /api/v1/capabilities/types
     */
    @GetMapping("/capabilities/types")
    public ResponseEntity<?> getAllCapabilityTypes() {
        try {
            return ResponseEntity.ok(PartnerCapabilityType.values());
        } catch (Exception e) {
            log.error("Error getting capability types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching capability types");
        }
    }

    /**
     * Get capability count for a vendor
     * GET /api/v1/vendors/{vendorId}/capabilities/count
     */
    @GetMapping("/vendors/{vendorId}/capabilities/count")
    public ResponseEntity<?> getVendorCapabilityCount(@PathVariable String vendorId) {
        try {
            long count = capabilityService.countByVendorId(vendorId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error getting capability count for vendor: {}", vendorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching capability count");
        }
    }
}
