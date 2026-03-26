package com.mirror.product.entity.misa;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "misa_inventory_items")
@Data
@EqualsAndHashCode(callSuper = false)
public class MisaInventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_item_code", unique = true, nullable = false)
    private String inventoryItemCode;

    @Column(name = "inventory_item_name")
    private String inventoryItemName;

    @Column(name = "inventory_item_id")
    private String inventoryItemId;

    @Column(name = "inventory_item_category_id")
    private String inventoryItemCategoryId;

    @Column(name = "inventory_item_category_name")
    private String inventoryItemCategoryName;

    @Column(name = "inventory_item_category_code")
    private String inventoryItemCategoryCode;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "unit_name")
    private String unitName;

    @Column(name = "unit_id")
    private String unitId;

    @Column(name = "sale_price", precision = 19, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "cost_price", precision = 19, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "quantity_on_hand")
    private Integer quantityOnHand;

    @Column(name = "quantity_reserved")
    private Integer quantityReserved;

    @Column(name = "quantity_available")
    private Integer quantityAvailable;

    @Column(name = "minimum_stock")
    private Integer minimumStock;

    @Column(name = "maximum_stock")
    private Integer maximumStock;

    @Column(name = "weight", precision = 10, scale = 3)
    private BigDecimal weight;

    @Column(name = "dimensions")
    private String dimensions;

    @Column(name = "color")
    private String color;

    @Column(name = "size")
    private String size;

    @Column(name = "material")
    private String material;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_for_sale")
    private Boolean isForSale = true;

    @Column(name = "is_for_purchase")
    private Boolean isForPurchase = true;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "warranty_period")
    private Integer warrantyPeriod;

    @Column(name = "warranty_unit")
    private String warrantyUnit;

    @Column(name = "origin_country")
    private String originCountry;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "barcode")
    private String barcode;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "misa_last_modified")
    private LocalDateTime misaLastModified;

    @Column(name = "last_sync_date")
    private LocalDateTime lastSyncDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "sync_status")
    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus = SyncStatus.PENDING;

    @Column(name = "sync_error_message", columnDefinition = "TEXT")
    private String syncErrorMessage;

    public enum SyncStatus {
        PENDING,
        SYNCED,
        ERROR,
        UPDATED
    }
}
