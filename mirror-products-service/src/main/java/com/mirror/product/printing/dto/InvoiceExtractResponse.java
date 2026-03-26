package com.mirror.product.printing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceExtractResponse {
    private boolean success;
    private InvoiceDataDto data;
    private String error;

    public static InvoiceExtractResponse success(InvoiceDataDto data) {
        return InvoiceExtractResponse.builder()
                .success(true)
                .data(data)
                .build();
    }

    public static InvoiceExtractResponse error(String message) {
        return InvoiceExtractResponse.builder()
                .success(false)
                .error(message)
                .build();
    }
}
