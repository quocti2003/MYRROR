package com.mirror.product.printing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemDto {
    private int stt;
    private String name;
    private String unit;
    private int quantity;
    private Long unitPrice;
    private Long total;
}
