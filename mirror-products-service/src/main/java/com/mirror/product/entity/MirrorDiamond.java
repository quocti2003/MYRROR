package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mirror Diamond Inventory
 * Stores individual lab-grown diamonds with Mirror SKU format:
 * LGD-{COLOR}{CLARITY}-{CARAT}-{SHAPE}-{ORIGIN}-{MFR}-{CERT}
 */
@Entity
@Table(name = "mirror_diamonds")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MirrorDiamond {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Mirror SKU Code (extended length for diamonds)
    @Column(name = "sku_code", unique = true, nullable = false, length = 50)
    private String skuCode; // e.g., LGD-FVS1-213-RD-IN-KARP-493218

    // Diamond Specifications
    @Column(name = "color", length = 2, nullable = false)
    private String color; // D, E, F, G, H, etc.

    @Column(name = "clarity", length = 10, nullable = false)
    private String clarity; // FL, IF, VVS1, VVS2, VS1, VS2, SI1, SI2

    @Column(name = "carat_weight", precision = 5, scale = 2, nullable = false)
    private BigDecimal caratWeight; // 2.13, 3.32, etc.

    @Column(name = "shape", length = 20, nullable = false)
    @Builder.Default
    private String shape = "ROUND"; // ROUND, PRINCESS, CUSHION, etc.

    // Origin & Manufacturer
    @Column(name = "origin_country", length = 2)
    private String originCountry; // IN, CN, US, etc.

    @Column(name = "manufacturer_code", length = 10)
    private String manufacturerCode; // KARP, SOLI, NDT, etc.

    @Column(name = "manufacturer_name", length = 200)
    private String manufacturerName; // Full name

    @Column(name = "manufacturer_serial", length = 50)
    private String manufacturerSerial; // 1146846, 1149542, etc.

    // Certification
    @Column(name = "cert_number", unique = true, nullable = false, length = 20)
    private String certNumber; // Full: 636493218

    @Column(name = "cert_lab", length = 20)
    private String certLab; // IGI, GIA, GCAL, etc.

    @Column(name = "cert_url", length = 500)
    private String certUrl; // Link to online certificate

    // Pricing (in USD)
    @Column(name = "unit_price_usd", precision = 10, scale = 2)
    private BigDecimal unitPriceUsd;

    @Column(name = "total_price_usd", precision = 10, scale = 2)
    private BigDecimal totalPriceUsd;

    // Import Information
    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber; // 312521770

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "customs_declaration", length = 50)
    private String customsDeclaration; // 107650143530

    @Column(name = "import_date")
    private LocalDate importDate;

    // Status
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "IN_STOCK"; // IN_STOCK, RESERVED, SOLD, RETURNED

    @Column(name = "location", length = 100)
    private String location; // Warehouse location

    // Optional: Link to mirror_products for unified inventory
    @Column(name = "mirror_product_id")
    private Long mirrorProductId; // FK to mirror_products.id

    // Metadata
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
