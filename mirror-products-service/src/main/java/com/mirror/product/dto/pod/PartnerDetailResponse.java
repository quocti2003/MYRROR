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
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerDetailResponse {

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

    // PHYGITAL fields
    private PartnerType partnerType;
    private BigDecimal wholesaleDiscountRate;
    private String territory;
    private Boolean canSetOwnPrices;
    private BigDecimal minMarkupPercent;
    private BigDecimal maxMarkupPercent;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private BigDecimal securityDeposit;
    private String bankAccountNumber;
    private String bankName;
    private String bankBranch;

    private Long userId;
    private String username;
    private String notes;
    private Instant approvedAt;
    private String approvedBy;
    private Instant createdAt;
    private Instant updatedAt;

    // Statistics
    private Integer totalPods;
    private Integer activePods;
    private Long totalScans;
    private Long totalAttributions;
    private BigDecimal totalRevenue;
    private BigDecimal totalCommissionsPaid;
    private BigDecimal pendingCommissions;

    public static PartnerDetailResponse fromEntity(PodPartner entity) {
        if (entity == null) {
            return null;
        }
        return PartnerDetailResponse.builder()
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
                .wholesaleDiscountRate(entity.getWholesaleDiscountRate())
                .territory(entity.getTerritory())
                .canSetOwnPrices(entity.getCanSetOwnPrices())
                .minMarkupPercent(entity.getMinMarkupPercent())
                .maxMarkupPercent(entity.getMaxMarkupPercent())
                .contractStartDate(entity.getContractStartDate())
                .contractEndDate(entity.getContractEndDate())
                .securityDeposit(entity.getSecurityDeposit())
                .bankAccountNumber(entity.getBankAccountNumber())
                .bankName(entity.getBankName())
                .bankBranch(entity.getBankBranch())
                .userId(entity.getUserId())
                .notes(entity.getNotes())
                .approvedAt(entity.getApprovedAt())
                .approvedBy(entity.getApprovedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PartnerDetailResponse withStatistics(
            Integer totalPods,
            Integer activePods,
            Long totalScans,
            Long totalAttributions,
            BigDecimal totalRevenue,
            BigDecimal totalCommissionsPaid,
            BigDecimal pendingCommissions
    ) {
        this.totalPods = totalPods;
        this.activePods = activePods;
        this.totalScans = totalScans;
        this.totalAttributions = totalAttributions;
        this.totalRevenue = totalRevenue;
        this.totalCommissionsPaid = totalCommissionsPaid;
        this.pendingCommissions = pendingCommissions;
        return this;
    }
}
