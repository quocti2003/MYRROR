package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.mirror.product.enums.VendorType;
import com.mirror.product.util.SequenceIdGenerator;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.config.ApplicationContextProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Vendor extends BaseEntity {
    
    @Column(nullable = false, unique = true)
    private String code; // Vendor code (e.g., VEN001)
    
    @Column(nullable = false)
    private String name;
    
    // User relationship - links vendor to user from User Service
    @Column(name = "owner_user_id")
    private String ownerUserId; // User ID from User Service JWT token
    
    // Location and Origin
    @Column(nullable = false)
    private String country; // Vendor's location/office country
    
    @Column(name = "country_of_origin")
    private String countryOfOrigin; // Where products are manufactured
    
    // Payment and Terms
    @Column(name = "payment_terms")
    private String paymentTerms;
    
    @Column(name = "commission_term", columnDefinition = "TEXT")
    private String commissionTerm; // Commission terms for vendor
    
    // Tax Structure
    @Column(name = "import_tax_percent", precision = 5, scale = 2)
    private BigDecimal importTaxPercent;
    
    @Column(name = "vat_tax_percent", precision = 5, scale = 2)
    private BigDecimal vatTaxPercent;
    
    @Column(name = "tax_customs_percent", precision = 5, scale = 2)
    private BigDecimal taxCustomsPercent;
    
    // Cost Structure
    @Column(name = "shipping_fee", precision = 10, scale = 2)
    private BigDecimal shippingFee;
    
    @Column(name = "avg_labor_cost_per_piece", precision = 10, scale = 2)
    private BigDecimal avgLaborCostPerPiece;
    
    @Column(name = "avg_product_cost", precision = 10, scale = 2)
    private BigDecimal avgProductCost; // Based on latest quote
    
    @Column(name = "last_quote_date")
    private LocalDateTime lastQuoteDate;
    
    // Operational Details
    @Column(name = "production_lead_time_days")
    private Integer productionLeadTimeDays;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_type")
    private VendorType vendorType;
    
    // Labor Cost Differentials (JSON or separate table)
    @Column(name = "labor_cost_factors", columnDefinition = "TEXT")
    private String laborCostFactors;
    
    // Contact Information
    @Column(name = "contact_person")
    private String contactPerson;
    
    @Column(name = "contact_email")
    private String contactEmail;
    
    @Column(name = "contact_phone")
    private String contactPhone;
    
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    
    // Relationships
    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<VendorProduct> vendorProducts;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<PartnerCapability> partnerCapabilities;
    
    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.VEN));
        }
    }
}