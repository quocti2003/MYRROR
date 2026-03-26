package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.mirror.product.enums.ProductStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 500, message = "Product name must not exceed 500 characters")
    private String name;

    private String description;

    // Category field - maps to MirrorProduct.category
    @JsonAlias("categoryId") // Support categoryId from frontend
    private String category;

    // SKU Code field - maps to MirrorProduct.skuCode (mirror_code column)
    @Size(max = 100, message = "SKU code must not exceed 100 characters")
    private String skuCode;

    // Validation temporarily disabled for product fulfillment workflow
    // @NotNull(message = "Price is required")
    // @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @Size(max = 3, message = "Currency code must not exceed 3 characters")
    private String currency = "VND";

    @Size(max = 50, message = "Metal type must not exceed 50 characters")
    private String metalType;

    @Size(max = 20, message = "Metal purity must not exceed 20 characters")
    private String metalPurity;

    @Size(max = 50, message = "Stone type must not exceed 50 characters")
    private String stoneType;

    @DecimalMin(value = "0.0", message = "Weight must be non-negative")
    private BigDecimal weightGrams;

    private String dimensions; // JSON string for product dimensions

    @Size(max = 1000, message = "Image URL must not exceed 1000 characters")
    private String imageUrl;

    private List<String> imageUrls; // Will be converted to JSON

    private List<String> tags; // Will be converted to JSON

    private ProductStatus status = ProductStatus.DRAFT;

    private Boolean featured = false;

    @Min(value = 0, message = "Stock quantity must be non-negative")
    private Integer stockQuantity = 0;

    @Min(value = 0, message = "Minimum stock level must be non-negative")
    private Integer minStockLevel = 1;

    // 3D Model ID - iJewel Drive model ID for 3D viewer
    @Size(max = 100, message = "3D Model ID must not exceed 100 characters")
    private String model3dId;

    // Flag to indicate if 3D model is not required for this product
    private Boolean model3dNotRequired = false;

    // Vendor assignment (optional)
    private String vendorId; // Optional - if provided, create/update VendorProduct relationship

    // Certificate codes list (optional) - multiple certificates can be linked to a product
    private List<String> certificateCodes;

    // Flag to indicate if certificates are not required for this product
    private Boolean certificatesNotRequired = false;

    // Constructors
    public ProductRequest() {}

    public ProductRequest(String name, String description, String category, BigDecimal price) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
    }

    @Override
    public String toString() {
        return "ProductRequest{" +
                "name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", skuCode='" + skuCode + '\'' +
                ", price=" + price +
                ", status=" + status +
                '}';
    }
}
