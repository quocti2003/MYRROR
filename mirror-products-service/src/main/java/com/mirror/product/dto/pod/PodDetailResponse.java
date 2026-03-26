package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.Pod;
import com.mirror.product.enums.PodStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodDetailResponse {

    private String id;
    private String partnerId;
    private String partnerName;
    private String name;
    private String description;
    private String locationName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String fullAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationId;
    private PodStatus status;
    private Integer displayCapacity;
    private BigDecimal commissionRate;
    private LocalDate installationDate;
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    // Products in this POD
    private List<PodProductInfo> products;

    // Statistics
    private Long totalScans;
    private Long uniqueScans;
    private Long totalAttributions;
    private BigDecimal totalRevenue;
    private Double conversionRate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PodProductInfo {
        private String productId;
        private String productName;
        private String productSku;
        private String qrCodeId;
        private String qrShortCode;
        private Long scanCount;
    }

    public static PodDetailResponse fromEntity(Pod entity) {
        if (entity == null) {
            return null;
        }
        return PodDetailResponse.builder()
                .id(entity.getId())
                .partnerId(entity.getPartnerId())
                .partnerName(entity.getPartner() != null ? entity.getPartner().getBusinessName() : null)
                .name(entity.getName())
                .description(entity.getDescription())
                .locationName(entity.getLocationName())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .city(entity.getCity())
                .state(entity.getState())
                .postalCode(entity.getPostalCode())
                .country(entity.getCountry())
                .fullAddress(entity.getFullAddress())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .locationId(entity.getLocationId())
                .status(entity.getStatus())
                .displayCapacity(entity.getDisplayCapacity())
                .commissionRate(entity.getCommissionRate())
                .installationDate(entity.getInstallationDate())
                .lastMaintenanceDate(entity.getLastMaintenanceDate())
                .nextMaintenanceDate(entity.getNextMaintenanceDate())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PodDetailResponse withStatistics(
            Long totalScans,
            Long uniqueScans,
            Long totalAttributions,
            BigDecimal totalRevenue,
            Double conversionRate
    ) {
        this.totalScans = totalScans;
        this.uniqueScans = uniqueScans;
        this.totalAttributions = totalAttributions;
        this.totalRevenue = totalRevenue;
        this.conversionRate = conversionRate;
        return this;
    }

    public PodDetailResponse withProducts(List<PodProductInfo> products) {
        this.products = products;
        return this;
    }
}
