package com.mirror.product.dto.diamond;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for lab-grown diamonds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiamondResponse {

    private String id;
    private String skuCode;

    // Diamond Specifications
    private String color;
    private String clarity;
    private BigDecimal caratWeight;
    private String shape;

    // Origin & Manufacturer
    private String originCountry;
    private String manufacturerCode;
    private String manufacturerName;
    private String manufacturerSerial;

    // Certification
    private String certNumber;
    private String certLab;
    private String certUrl;

    // Pricing
    private BigDecimal unitPriceUsd;
    private BigDecimal totalPriceUsd;

    // Import Information
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String customsDeclaration;
    private LocalDate importDate;

    // Status
    private String status;
    private String location;
    private Long mirrorProductId;

    // Metadata
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}
