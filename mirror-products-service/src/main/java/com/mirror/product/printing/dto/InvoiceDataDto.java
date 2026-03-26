package com.mirror.product.printing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDataDto {
    private String invoiceCode;
    private String invoiceNumber;
    private String invoiceDate;
    private String paymentMethod;
    private String customerCompany;
    private String customerName;
    private String customerTaxCode;
    private String customerAddress;
    private String customerPhone;
    private String customerIdNumber;
    private List<InvoiceItemDto> items;
    private long subtotal;
    private String totalInWords;
}
