package com.mirror.product.dto.jtrc;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new JTRC (Jewelry Technical Report Card)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JTRCCreateRequest {

    // Optional: link to existing collection plan item
    private String collectionPlanItemId;

    // Optional: link to existing product
    private String productId;

    // === Admin Header (Required) ===
    @NotBlank(message = "Collection is required")
    @Size(max = 100, message = "Collection must not exceed 100 characters")
    private String collection;

    @NotBlank(message = "Season is required")
    @Size(max = 20, message = "Season must not exceed 20 characters")
    private String season;

    @Size(max = 50, message = "Project ID must not exceed 50 characters")
    private String projectId;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @Size(max = 100, message = "Source must not exceed 100 characters")
    private String source;

    private LocalDate entryDate;

    // === Pricing Snapshot ===
    @DecimalMin(value = "0.0", message = "Gold price per gram must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Gold price must have at most 13 integer digits and 2 decimal places")
    private BigDecimal goldPricePerGram;

    @DecimalMin(value = "0.0", message = "Exchange rate must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Exchange rate must have at most 13 integer digits and 2 decimal places")
    private BigDecimal exchangeRateUsd;

    // === Production Metrics ===
    @Min(value = 1, message = "Production difficulty must be at least 1")
    @Max(value = 5, message = "Production difficulty must not exceed 5")
    private Integer productionDifficulty;

    @Min(value = 0, message = "Estimated lead time must be non-negative")
    private Integer estimatedLeadTimeDays;

    @Size(max = 50, message = "Casting status must not exceed 50 characters")
    private String castingStatus;

    @Size(max = 2000, message = "Production notes must not exceed 2000 characters")
    private String productionNotes;

    // === Visual Assets (S3 URLs) ===
    private String render3dUrl;
    private String stoneMapUrl;
    private String technicalDrawingUrl;

    // === Components ===
    @Valid
    private JTRCMetalComponentDTO metalComponent;

    @Valid
    private List<JTRCStoneComponentDTO> stoneComponents;

    @Valid
    private List<JTRCLaborComponentDTO> laborComponents;
}
