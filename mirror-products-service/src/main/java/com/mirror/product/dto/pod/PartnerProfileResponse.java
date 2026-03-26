package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.PodPartner;
import com.mirror.product.enums.BusinessType;
import com.mirror.product.enums.PartnerStatus;
import com.mirror.product.enums.PartnerTier;
import com.mirror.product.enums.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Partner profile information for their portal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerProfileResponse {

    private String id;
    private String businessName;
    private BusinessType businessType;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String businessLicense;
    private String taxId;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String fullAddress;
    private PartnerStatus status;
    private PartnerTier tier;
    private BigDecimal commissionRate;
    private PartnerType partnerType;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static PartnerProfileResponse fromEntity(PodPartner entity) {
        if (entity == null) {
            return null;
        }
        return PartnerProfileResponse.builder()
                .id(entity.getId())
                .businessName(entity.getBusinessName())
                .businessType(entity.getBusinessType())
                .contactName(entity.getContactName())
                .contactEmail(entity.getContactEmail())
                .contactPhone(entity.getContactPhone())
                .businessLicense(entity.getBusinessLicense())
                .taxId(entity.getTaxId())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .city(entity.getCity())
                .state(entity.getState())
                .postalCode(entity.getPostalCode())
                .country(entity.getCountry())
                .fullAddress(entity.getFullAddress())
                .status(entity.getStatus())
                .tier(entity.getTier())
                .commissionRate(entity.getCommissionRate())
                .partnerType(entity.getPartnerType())
                .approvedAt(entity.getApprovedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
