package com.mirror.product.dto.partnercapability;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.PartnerCapabilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for Partner Capability
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartnerCapabilityDTO {

    private String id;
    private String vendorId;
    private String vendorName;
    private String vendorCode;

    private PartnerCapabilityType capabilityType;
    private Integer leadTimeDays;
    private BigDecimal costPerPiece;
    private BigDecimal costPerGram;
    private Integer qualityRating;
    private String notes;

    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
