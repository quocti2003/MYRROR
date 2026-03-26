package com.mirror.product.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Material Inventory - Raw materials and components for jewelry production
 * Migrated from mirror-mrp-service Material entity
 */
@Entity
@Table(name = "material_inventory")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Vendor vendor;

    @Column(name = "base_price", precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "market_price", precision = 10, scale = 2)
    private BigDecimal marketPrice;

    @Column(nullable = false)
    private String currency;

    @Column(name = "tax_customs_percent", precision = 5, scale = 2)
    private BigDecimal taxCustomsPercent;

    @Column(name = "assembly_cost_local", precision = 10, scale = 2)
    private BigDecimal assemblyCostLocal;

    @Column(name = "delivery_lead_time_days")
    private Integer deliveryLeadTimeDays;

    @Column(name = "production_lead_time_days")
    private Integer productionLeadTimeDays;

    @Column(name = "assembly_lead_time_days")
    private Integer assemblyLeadTimeDays;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<PurchaseOrderItem> purchaseOrderItems;

    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<PurchaseOrderItemVariant> orderItemVariants;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
