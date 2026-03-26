package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.PartnerSale;
import com.mirror.product.enums.PartnerSaleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerSaleResponse {

    private String id;
    private String saleNumber;
    private String partnerId;
    private String podId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private PartnerSaleStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal costOfGoods;
    private BigDecimal profitAmount;
    private BigDecimal profitMarginPercent;
    private String currency;
    private String paymentMethod;
    private String paymentReference;
    private String qrCodeId;
    private String notes;
    private Instant soldAt;
    private List<PartnerSaleItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;

    public static PartnerSaleResponse fromEntity(PartnerSale entity) {
        if (entity == null) return null;
        PartnerSaleResponseBuilder builder = PartnerSaleResponse.builder()
                .id(entity.getId())
                .saleNumber(entity.getSaleNumber())
                .partnerId(entity.getPartnerId())
                .podId(entity.getPodId())
                .customerName(entity.getCustomerName())
                .customerPhone(entity.getCustomerPhone())
                .customerEmail(entity.getCustomerEmail())
                .status(entity.getStatus())
                .subtotal(entity.getSubtotal())
                .discountAmount(entity.getDiscountAmount())
                .taxAmount(entity.getTaxAmount())
                .totalAmount(entity.getTotalAmount())
                .costOfGoods(entity.getCostOfGoods())
                .profitAmount(entity.getProfitAmount())
                .profitMarginPercent(entity.getProfitMarginPercent())
                .currency(entity.getCurrency())
                .paymentMethod(entity.getPaymentMethod())
                .paymentReference(entity.getPaymentReference())
                .qrCodeId(entity.getQrCodeId())
                .notes(entity.getNotes())
                .soldAt(entity.getSoldAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        if (entity.getItems() != null) {
            builder.items(entity.getItems().stream()
                    .map(PartnerSaleItemResponse::fromEntity)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
