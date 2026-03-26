package com.mirror.product.mapper;

import com.mirror.product.dto.partnercapability.PartnerCapabilityCreateRequest;
import com.mirror.product.dto.partnercapability.PartnerCapabilityDTO;
import com.mirror.product.dto.partnercapability.PartnerCapabilityUpdateRequest;
import com.mirror.product.dto.partnercapability.VendorWithCapabilityResponse;
import com.mirror.product.entity.PartnerCapability;
import com.mirror.product.entity.Vendor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Partner Capability entities and DTOs
 */
@Component
public class PartnerCapabilityMapper {

    public PartnerCapabilityDTO toDTO(PartnerCapability entity) {
        if (entity == null) {
            return null;
        }

        PartnerCapabilityDTO.PartnerCapabilityDTOBuilder builder = PartnerCapabilityDTO.builder()
                .id(entity.getId())
                .capabilityType(entity.getCapabilityType())
                .leadTimeDays(entity.getLeadTimeDays())
                .costPerPiece(entity.getCostPerPiece())
                .costPerGram(entity.getCostPerGram())
                .qualityRating(entity.getQualityRating())
                .notes(entity.getNotes())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        // Add vendor information if available
        if (entity.getVendor() != null) {
            builder.vendorId(entity.getVendor().getId())
                   .vendorName(entity.getVendor().getName())
                   .vendorCode(entity.getVendor().getCode());
        }

        return builder.build();
    }

    public PartnerCapability toEntity(PartnerCapabilityCreateRequest request, Vendor vendor) {
        if (request == null) {
            return null;
        }

        return PartnerCapability.builder()
                .vendor(vendor)
                .capabilityType(request.getCapabilityType())
                .leadTimeDays(request.getLeadTimeDays())
                .costPerPiece(request.getCostPerPiece())
                .costPerGram(request.getCostPerGram())
                .qualityRating(request.getQualityRating())
                .notes(request.getNotes())
                .isActive(true)
                .build();
    }

    public void updateEntity(PartnerCapability entity, PartnerCapabilityUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getLeadTimeDays() != null) {
            entity.setLeadTimeDays(request.getLeadTimeDays());
        }
        if (request.getCostPerPiece() != null) {
            entity.setCostPerPiece(request.getCostPerPiece());
        }
        if (request.getCostPerGram() != null) {
            entity.setCostPerGram(request.getCostPerGram());
        }
        if (request.getQualityRating() != null) {
            entity.setQualityRating(request.getQualityRating());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }
    }

    public List<PartnerCapabilityDTO> toDTOList(List<PartnerCapability> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert PartnerCapability to VendorWithCapabilityResponse
     * Used for capability-based vendor search (P3-02)
     */
    public VendorWithCapabilityResponse toVendorWithCapabilityResponse(PartnerCapability entity) {
        if (entity == null) {
            return null;
        }

        VendorWithCapabilityResponse.VendorWithCapabilityResponseBuilder builder = VendorWithCapabilityResponse.builder()
                .capabilityId(entity.getId())
                .capabilityType(entity.getCapabilityType())
                .leadTimeDays(entity.getLeadTimeDays())
                .costPerPiece(entity.getCostPerPiece())
                .costPerGram(entity.getCostPerGram())
                .qualityRating(entity.getQualityRating())
                .capabilityNotes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isActive(entity.getIsActive());

        // Add vendor information if available
        Vendor vendor = entity.getVendor();
        if (vendor != null) {
            builder.vendorId(vendor.getId())
                   .vendorCode(vendor.getCode())
                   .vendorName(vendor.getName())
                   .vendorType(vendor.getVendorType())
                   .country(vendor.getCountry())
                   .countryOfOrigin(vendor.getCountryOfOrigin())
                   .contactPerson(vendor.getContactPerson())
                   .contactEmail(vendor.getContactEmail())
                   .contactPhone(vendor.getContactPhone());
        }

        return builder.build();
    }

    /**
     * Convert list of PartnerCapability to VendorWithCapabilityResponse list
     */
    public List<VendorWithCapabilityResponse> toVendorWithCapabilityResponseList(List<PartnerCapability> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toVendorWithCapabilityResponse)
                .collect(Collectors.toList());
    }
}
