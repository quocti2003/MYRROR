package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {
    private String id;
    private String name;
    private String description;

    // Category name - human readable name from misa_product_categories
    private String categoryName;

    // SKU code field - from MirrorProduct.skuCode (mirror_code column)
    private String skuCode;

    private SkuSummary sku; // Nested SKU info (for detailed category data if needed)
    private CategorySummary categoryDetails; // Detailed category info
    private VendorResponse vendor; // Nested vendor info
    private BigDecimal price;
    private String currency;
    private String metalType;
    private String metalPurity;
    private String stoneType;
    private BigDecimal weightGrams;
    private Object dimensions; // Parsed JSON object

    // Jewelry specification fields (from SKU generation)
    private String materialColor;
    private Boolean isCoated;
    private String coatingMaterial;
    private String stoneShape;
    private String stoneWeight;
    private String stoneOrigin;
    private String sideStones;
    private String countryOfOrigin;
    private String materialWeight;
    private String barcode;
    private String imageUrl;
    private List<String> imageUrls; // Parsed JSON array
    private List<String> tags; // Parsed JSON array
    private ProductStatus status;
    private String model3dId; // iJewel Drive model ID for 3D viewer
    private Boolean model3dNotRequired; // Flag indicating if 3D model is not required
    private LocalDateTime publishedAt;
    private LocalDateTime fulfillmentCompletedAt;
    private Boolean featured;
    private Integer stockQuantity;
    private Integer minStockLevel;
    private Boolean inStock;
    private Boolean lowStock;
    private Boolean available;
    private List<String> certificateCodes; // Multiple certificates linked to this product
    private Boolean certificatesNotRequired; // Flag indicating if certificates are not required
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public ProductResponse() {}

    public ProductResponse(MirrorProduct product) {
        this.id = String.valueOf(product.getId());
        this.name = product.getItemName();
        this.description = product.getDescription();
        this.skuCode = product.getSkuCode();
        this.price = product.getPrice();
        this.currency = product.getCurrency();
        this.metalType = product.getMetalType();
        this.metalPurity = product.getMetalPurity();
        this.stoneType = product.getStoneType();
        this.weightGrams = product.getWeightGrams();
        this.imageUrl = product.getImageUrl();
        this.status = product.getStatus();
        this.model3dId = product.getModel3dId();
        this.model3dNotRequired = product.getModel3dNotRequired();
        this.featured = product.getFeatured();
        this.stockQuantity = product.getStockQuantity();
        this.minStockLevel = product.getMinStockLevel();
        this.certificatesNotRequired = product.getCertificatesNotRequired();
        // Jewelry specification fields
        this.materialColor = product.getMaterialColor();
        this.isCoated = product.getIsCoated();
        this.coatingMaterial = product.getCoatingMaterial();
        this.stoneShape = product.getStoneShape();
        this.stoneWeight = product.getStoneWeight();
        this.stoneOrigin = product.getStoneOrigin();
        this.sideStones = product.getSideStones();
        this.countryOfOrigin = product.getCountryOfOrigin();
        this.materialWeight = product.getMaterialWeight();
        this.barcode = product.getBarcode();
        // certificateCodes will be loaded from join table by ProductMapper
        this.createdAt = product.getCreatedAt() != null ?
            Instant.from(product.getCreatedAt().atZone(java.time.ZoneId.systemDefault())) : null;
        this.updatedAt = product.getUpdatedAt() != null ?
            Instant.from(product.getUpdatedAt().atZone(java.time.ZoneId.systemDefault())) : null;
    }

    // Nested class for SKU info
    public static class SkuSummary {
        private String id;
        private String name;
        private String description;

        public SkuSummary() {}

        public SkuSummary(com.mirror.product.entity.MirrorProduct product) {
            this.id = product.getId();
            this.name = product.getItemName();
            this.description = product.getDescription();
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // Nested class for vendor info
    public static class VendorResponse {
        private String id;
        private String code;
        private String name;
        private String country;

        public VendorResponse() {}

        public VendorResponse(com.mirror.product.entity.Vendor vendor) {
            this.id = vendor.getId();
            this.code = vendor.getCode();
            this.name = vendor.getName();
            this.country = vendor.getCountry();
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }

    // Legacy nested class for backwards compatibility (category is an alias for SKU)
    @Data
    public static class CategorySummary {
        private String id;
        private String name;
        private String description;

        public CategorySummary() {}

        public CategorySummary(com.mirror.product.entity.MirrorProduct product) {
            this.id = product.getId();
            this.name = product.getItemName();
            this.description = product.getDescription();
        }
    }

    @Override
    public String toString() {
        return "ProductResponse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", skuCode='" + skuCode + '\'' +
                ", price=" + price +
                ", status=" + status +
                '}';
    }
}
