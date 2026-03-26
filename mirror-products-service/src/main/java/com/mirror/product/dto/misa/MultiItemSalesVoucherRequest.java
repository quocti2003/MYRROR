package com.mirror.product.dto.misa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiItemSalesVoucherRequest {

    private List<LineItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItem {
        private String itemCode;
        @Builder.Default
        private BigDecimal amount = new BigDecimal("1000000");
        @Builder.Default
        private BigDecimal costPrice = new BigDecimal("50000");
        @Builder.Default
        private int quantity = 1;
    }
}
