package com.mirror.product.dto.label;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RFIDScanResponse {

    private Boolean success;
    private String scanId;
    private Boolean tagFound;
    private String message;
    private RFIDTagResponse.ProductSummary product;
    private RFIDTagInfo tagInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RFIDTagInfo {
        private String epc;
        private String status;
        private Integer scanCount;
    }
}
