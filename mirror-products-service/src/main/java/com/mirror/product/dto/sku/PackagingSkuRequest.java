package com.mirror.product.dto.sku;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PackagingSkuRequest {
    @NotBlank(message = "Prefix is required")
    private String prefix;  // e.g., "BOX"

    private String size;      // e.g., "LARGE", "SMALL", "MEDIUM"
    private String material;  // e.g., "MICROFIBER", "LEATHERETTE", "VELVET"
    private String color;     // e.g., "NAVY", "RED", "BLACK", "BRIGHTSILVER"
    private String type;      // e.g., "RING", "NECKLACE", "BRACELET"
    private String finish;    // e.g., "MATTE", "GLOSSY"

    @NotBlank(message = "Country of origin is required")
    private String countryOfOrigin;  // e.g., "CN", "IN", "TH", "HK"

    @NotBlank(message = "Item name is required")
    private String itemName;  // Product name (required)

    private String notes;     // Additional specifications (optional)

    // Non-serialized inventory: stock quantity (shared barcode)
    private Integer stockQuantity = 0;

    private String stockLocation;    // Storage location
}
