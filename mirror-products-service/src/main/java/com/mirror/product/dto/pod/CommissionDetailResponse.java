package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.PodCommission;
import com.mirror.product.enums.CommissionStatus;
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
public class CommissionDetailResponse {

    private String id;
    private String partnerId;
    private String partnerName;
    private String partnerBusinessName;
    private String partnerEmail;
    private String partnerPhone;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer totalOrders;
    private BigDecimal totalOrderAmount;
    private BigDecimal attributedAmount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private String currency;
    private BigDecimal adjustments;
    private String adjustmentReason;
    private BigDecimal finalAmount;
    private CommissionStatus status;
    private Instant approvedAt;
    private String approvedBy;
    private Instant paidAt;
    private String paymentReference;
    private String paymentMethod;
    private String bankAccount;
    private String bankName;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    // Included attributions
    private List<AttributionSummary> attributions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributionSummary {
        private String id;
        private String orderId;
        private String podName;
        private BigDecimal orderAmount;
        private BigDecimal attributedAmount;
        private Instant orderPlacedAt;
        private Integer daysToConversion;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
    }

    public static CommissionDetailResponse fromEntity(PodCommission entity) {
        if (entity == null) {
            return null;
        }
        CommissionDetailResponseBuilder builder = CommissionDetailResponse.builder()
                .id(entity.getId())
                .partnerId(entity.getPartnerId())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .totalOrders(entity.getTotalOrders())
                .totalOrderAmount(entity.getTotalOrderAmount())
                .attributedAmount(entity.getAttributedAmount())
                .commissionRate(entity.getCommissionRate())
                .commissionAmount(entity.getCommissionAmount())
                .currency(entity.getCurrency())
                .adjustments(entity.getAdjustments())
                .adjustmentReason(entity.getAdjustmentReason())
                .finalAmount(entity.getFinalAmount())
                .status(entity.getStatus())
                .approvedAt(entity.getApprovedAt())
                .approvedBy(entity.getApprovedBy())
                .paidAt(entity.getPaidAt())
                .paymentReference(entity.getPaymentReference())
                .paymentMethod(entity.getPaymentMethod())
                .bankAccount(entity.getBankAccount())
                .bankName(entity.getBankName())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        if (entity.getPartner() != null) {
            builder.partnerName(entity.getPartner().getContactName())
                    .partnerBusinessName(entity.getPartner().getBusinessName())
                    .partnerEmail(entity.getPartner().getContactEmail())
                    .partnerPhone(entity.getPartner().getContactPhone());
        }

        return builder.build();
    }

    public CommissionDetailResponse withAttributions(List<AttributionSummary> attributions) {
        this.attributions = attributions;
        return this;
    }
}
