package com.mirror.product.dto.partnercapability;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.PartnerCapabilityType;
import com.mirror.product.enums.VendorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for vendors with specific capability details
 * Used by P3-02: Capability-based Partner Filter
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorWithCapabilityResponse {

    // Vendor info
    private String vendorId;
    private String vendorCode;
    private String vendorName;
    private VendorType vendorType;
    private String country;
    private String countryOfOrigin;

    // Capability details
    private String capabilityId;
    private PartnerCapabilityType capabilityType;
    private Integer leadTimeDays;
    private BigDecimal costPerPiece;
    private BigDecimal costPerGram;
    private Integer qualityRating;
    private String capabilityNotes;

    // Contact info
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isActive;
}
