package com.mirror.product.dto.label;

import com.mirror.product.entity.label.RFIDTag;
import com.mirror.product.enums.RFIDTagStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RFIDTagResponse {

    private String id;
    private String epc;
    private String productId;
    private String printJobId;
    private RFIDTagStatus status;
    private Integer scanCount;
    private Instant lastScannedAt;
    private String lastScannedDevice;
    private String lastScannedLocation;
    private Instant encodedAt;
    private Instant voidedAt;
    private String voidedReason;
    private Map<String, Object> metadata;
    private ProductSummary product;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummary {
        private String id;
        private String name;
        private String sku;
        private String barcode;
        private BigDecimal price;
        private String imageUrl;
    }

    public static RFIDTagResponse fromEntity(RFIDTag entity) {
        if (entity == null) return null;

        RFIDTagResponseBuilder builder = RFIDTagResponse.builder()
            .id(entity.getId())
            .epc(entity.getEpc())
            .productId(entity.getProductId())
            .printJobId(entity.getPrintJobId())
            .status(entity.getStatus())
            .scanCount(entity.getScanCount())
            .lastScannedAt(entity.getLastScannedAt())
            .lastScannedDevice(entity.getLastScannedDevice())
            .lastScannedLocation(entity.getLastScannedLocation())
            .encodedAt(entity.getEncodedAt())
            .voidedAt(entity.getVoidedAt())
            .voidedReason(entity.getVoidedReason())
            .metadata(entity.getMetadata());

        if (entity.getProduct() != null) {
            builder.product(ProductSummary.builder()
                .id(entity.getProduct().getId())
                .name(entity.getProduct().getItemName())
                .sku(entity.getProduct().getSkuCode())
                .barcode(entity.getProduct().getBarcode())
                .price(entity.getProduct().getPrice())
                .imageUrl(entity.getProduct().getImageUrl())
                .build());
        }

        return builder.build();
    }
}
