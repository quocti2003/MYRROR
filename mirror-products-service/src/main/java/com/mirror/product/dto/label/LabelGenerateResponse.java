package com.mirror.product.dto.label;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelGenerateResponse {

    private Integer totalLabels;
    private String combinedZpl; // All labels combined
    private List<LabelData> labels;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelData {
        private String productId;
        private String productName;
        private String sku;
        private String barcode;
        private String epc;
        private String zpl;
    }
}
