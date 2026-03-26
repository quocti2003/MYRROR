package com.mirror.product.dto.diamond;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for searching lab-grown diamonds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiamondSearchRequest {

    private String color;
    private String clarity;
    private String shape;
    private BigDecimal minCarat;
    private BigDecimal maxCarat;
    private String status;
    private String manufacturerCode;
    private String invoiceNumber;
}
