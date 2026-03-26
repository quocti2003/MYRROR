package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.entity.Vendor;
import com.mirror.product.enums.VendorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorResponse {
    
    private String id;
    private String code;
    private String name;
    private String country;
    private String countryOfOrigin;
    private String paymentTerms;
    private String commissionTerm;
    private BigDecimal importTaxPercent;
    private BigDecimal vatTaxPercent;
    private BigDecimal taxCustomsPercent;
    private BigDecimal shippingFee;
    private BigDecimal avgLaborCostPerPiece;
    private BigDecimal avgProductCost;
    private LocalDateTime lastQuoteDate;
    private Integer productionLeadTimeDays;
    private VendorType vendorType;
    private String laborCostFactors;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String ownerUserId; // Add this field for debugging
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer productCount;
    
    public VendorResponse(Vendor vendor) {
        this.id = vendor.getId();
        this.code = vendor.getCode();
        this.name = vendor.getName();
        this.country = vendor.getCountry();
        this.countryOfOrigin = vendor.getCountryOfOrigin();
        this.paymentTerms = vendor.getPaymentTerms();
        this.commissionTerm = vendor.getCommissionTerm();
        this.importTaxPercent = vendor.getImportTaxPercent();
        this.vatTaxPercent = vendor.getVatTaxPercent();
        this.taxCustomsPercent = vendor.getTaxCustomsPercent();
        this.shippingFee = vendor.getShippingFee();
        this.avgLaborCostPerPiece = vendor.getAvgLaborCostPerPiece();
        this.avgProductCost = vendor.getAvgProductCost();
        this.lastQuoteDate = vendor.getLastQuoteDate();
        this.productionLeadTimeDays = vendor.getProductionLeadTimeDays();
        this.vendorType = vendor.getVendorType();
        this.laborCostFactors = vendor.getLaborCostFactors();
        this.contactPerson = vendor.getContactPerson();
        this.contactEmail = vendor.getContactEmail();
        this.contactPhone = vendor.getContactPhone();
        this.address = vendor.getAddress();
        this.ownerUserId = vendor.getOwnerUserId(); // Add this mapping
        this.isActive = vendor.getIsActive();
        this.createdAt = vendor.getCreatedAt();
        this.updatedAt = vendor.getUpdatedAt();
        
        if (vendor.getVendorProducts() != null) {
            this.productCount = vendor.getVendorProducts().size();
        }
    }
}