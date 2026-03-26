package com.mirror.product.dto.sku;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JewelrySkuRequest {
    @NotBlank(message = "Prefix is required")
    private String prefix;  // e.g., "RNG", "EAR", "NCK"

    @NotBlank(message = "Material is required")
    private String material;    // e.g., "18KWHITEGOLD", "18KYELLOWGOLD", "SILVER"

    @NotBlank(message = "Material color is required")
    private String materialColor;  // e.g., "WHITE", "YELLOW", "ROSE"

    @NotBlank(message = "Material weight is required")
    private String materialWeight;  // e.g., "5.2G", "3.8G"

    @NotNull(message = "Coating status is required")
    private Boolean isCoated;  // true if material is coated (applicable for silver)

    private String coatingMaterial;  // e.g., "RHODIUM", "GOLD" (required if isCoated = true)

    @NotBlank(message = "Stone origin is required")
    private String origin;      // e.g., "LABGROWN", "NATURAL"

    @NotBlank(message = "Stone shape is required")
    private String shape;       // e.g., "PEAR", "ROUND", "OVAL"

    @NotBlank(message = "Stone weight is required")
    private String weight;      // e.g., "7.98CT", "2.00CT"

    @NotBlank(message = "Side stones is required")
    private String sideStones;  // e.g., "DIAMONDS", "NONE"

    @NotBlank(message = "Country of origin is required")
    private String countryOfOrigin;  // e.g., "CN", "IN", "TH", "HK"

    @NotBlank(message = "Item name is required")
    private String itemName;    // Product name (required)

    private String variant;     // Additional notes (optional)

    // Serialized inventory: number of units to create (each with unique barcode)
    // Default is 1. Set higher to create multiple units at once.
    private Integer unitQuantity = 1;

    private String unitLocation;     // Storage location for units

    private java.math.BigDecimal unitCostPrice;   // Cost price per unit

    private java.math.BigDecimal unitSalePrice;   // Sale price per unit
}
