package com.mirror.product.entity;

import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.ProductStatus;
import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "mirror_products")
@Data
public class MirrorProduct {
    @Id
    @Column(name = "id", length = 30)
    private String id;

    @Column(name = "mirror_code", unique = true, nullable = false, length = 30)
    private String skuCode;

    @Column(name = "misa_item_code")
    private String misaItemCode;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "item_name", length = 500)
    private String itemName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", length = 3)
    private String currency = "VND";

    @Column(name = "cost", precision = 19, scale = 2)
    private BigDecimal cost;

    // Jewelry-specific fields
    @Column(name = "metal_type", length = 50)
    private String metalType;

    @Column(name = "metal_purity", length = 20)
    private String metalPurity;

    @Column(name = "stone_type", length = 50)
    private String stoneType;

    @Column(name = "weight_grams", precision = 8, scale = 3)
    private BigDecimal weightGrams;

    @Column(name = "dimensions", columnDefinition = "TEXT")
    private String dimensions;

    // Additional jewelry specification fields (from SKU generation)
    @Column(name = "material_color", length = 50)
    private String materialColor;

    @Column(name = "is_coated")
    private Boolean isCoated = false;

    @Column(name = "coating_material", length = 50)
    private String coatingMaterial;

    @Column(name = "stone_shape", length = 50)
    private String stoneShape;

    @Column(name = "stone_weight", length = 50)
    private String stoneWeight;

    @Column(name = "stone_origin", length = 50)
    private String stoneOrigin;

    @Column(name = "side_stones", length = 100)
    private String sideStones;

    @Column(name = "country_of_origin", length = 10)
    private String countryOfOrigin;

    @Column(name = "material_weight", length = 50)
    private String materialWeight;

    // Descriptive code for human-readable product specs (e.g., RNG-18KWG-W-2.09-LG-RD-1.29-N)
    @Column(name = "descriptive_code", length = 100)
    private String descriptiveCode;

    // Image fields
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    // Design fields
    @Column(name = "design_id", length = 100)
    private String designId;

    // 3D Model field - iJewel Drive model ID for 3D viewer
    @Column(name = "model_3d_id", length = 100)
    private String model3dId;

    // Flag to indicate if 3D model is not required for this product
    @Column(name = "model_3d_not_required")
    private Boolean model3dNotRequired = false;

    @Column(name = "designer_id")
    private Long designerId;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Column(name = "min_stock_level")
    private Integer minStockLevel = 1;

    @Column(name = "location")
    private String location;

    @Column(name = "barcode", unique = true, length = 100)
    private String barcode;

    // Many-to-many relationship with certificates via join table
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ProductCertificate> productCertificates = new HashSet<>();

    // Flag to indicate if certificates are not required for this product
    @Column(name = "certificates_not_required")
    private Boolean certificatesNotRequired = false;

    // MISA integration fields (absorbed from Sku entity)
    @Column(name = "misa_category_id")
    private String misaCategoryId;

    @Column(name = "misa_category_code")
    private String misaCategoryCode;

    @Column(name = "misa_category_name")
    private String misaCategoryName;

    @Column(name = "misa_inventory_id", length = 100)
    private String misaInventoryId;

    @Column(name = "misa_synced", nullable = false)
    private Boolean misaSynced = false;

    @Column(name = "misa_sync_date")
    private LocalDateTime misaSyncDate;

    @Column(name = "misa_last_synced_at")
    private Instant misaLastSyncedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "fulfillment_completed_at")
    private LocalDateTime fulfillmentCompletedAt;

    @Column(name = "featured")
    private Boolean featured = false;

    // Component relationship (absorbed from Sku entity)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Component> components = new HashSet<>();

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        // Generate ID if not set
        if (id == null || id.isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            id = sequenceIdGenerator.generateId(EntityPrefix.PRD);
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ProductStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
