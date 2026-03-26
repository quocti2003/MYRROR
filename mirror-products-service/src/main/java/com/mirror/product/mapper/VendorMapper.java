package com.mirror.product.mapper;

import com.mirror.product.dto.VendorRequest;
import com.mirror.product.dto.VendorResponse;
import com.mirror.product.entity.Vendor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class VendorMapper {

    public Vendor toEntity(VendorRequest request) {
        if (request == null) {
            return null;
        }
        
        return Vendor.builder()
                .code(request.getCode())
                .name(request.getName())
                .country(request.getCountry())
                .countryOfOrigin(request.getCountryOfOrigin())
                .paymentTerms(request.getPaymentTerms())
                .commissionTerm(request.getCommissionTerm())
                .importTaxPercent(request.getImportTaxPercent())
                .vatTaxPercent(request.getVatTaxPercent())
                .taxCustomsPercent(request.getTaxCustomsPercent())
                .shippingFee(request.getShippingFee())
                .avgLaborCostPerPiece(request.getAvgLaborCostPerPiece())
                .avgProductCost(request.getAvgProductCost())
                .productionLeadTimeDays(request.getProductionLeadTimeDays())
                .vendorType(request.getVendorType())
                .laborCostFactors(request.getLaborCostFactors())
                .contactPerson(request.getContactPerson())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .isActive(true)
                .build();
    }

    public VendorResponse toResponse(Vendor entity) {
        if (entity == null) {
            return null;
        }
        
        return VendorResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .country(entity.getCountry())
                .countryOfOrigin(entity.getCountryOfOrigin())
                .paymentTerms(entity.getPaymentTerms())
                .commissionTerm(entity.getCommissionTerm())
                .importTaxPercent(entity.getImportTaxPercent())
                .vatTaxPercent(entity.getVatTaxPercent())
                .taxCustomsPercent(entity.getTaxCustomsPercent())
                .shippingFee(entity.getShippingFee())
                .avgLaborCostPerPiece(entity.getAvgLaborCostPerPiece())
                .avgProductCost(entity.getAvgProductCost())
                .lastQuoteDate(entity.getLastQuoteDate())
                .productionLeadTimeDays(entity.getProductionLeadTimeDays())
                .vendorType(entity.getVendorType())
                .laborCostFactors(entity.getLaborCostFactors())
                .contactPerson(entity.getContactPerson())
                .contactEmail(entity.getContactEmail())
                .contactPhone(entity.getContactPhone())
                .address(entity.getAddress())
                .ownerUserId(entity.getOwnerUserId()) // Add this line
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                // Avoid lazy loading vendorProducts - use 0 as default, count can be fetched separately if needed
                .productCount(0)
                .build();
    }

    public List<VendorResponse> toResponseList(List<Vendor> vendors) {
        if (vendors == null) {
            return null;
        }
        
        return vendors.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void updateEntity(Vendor entity, VendorRequest request) {
        if (entity == null || request == null) {
            return;
        }
        
        if (request.getCode() != null) {
            entity.setCode(request.getCode());
        }
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getCountry() != null) {
            entity.setCountry(request.getCountry());
        }
        if (request.getCountryOfOrigin() != null) {
            entity.setCountryOfOrigin(request.getCountryOfOrigin());
        }
        if (request.getPaymentTerms() != null) {
            entity.setPaymentTerms(request.getPaymentTerms());
        }
        if (request.getCommissionTerm() != null) {
            entity.setCommissionTerm(request.getCommissionTerm());
        }
        if (request.getImportTaxPercent() != null) {
            entity.setImportTaxPercent(request.getImportTaxPercent());
        }
        if (request.getVatTaxPercent() != null) {
            entity.setVatTaxPercent(request.getVatTaxPercent());
        }
        if (request.getTaxCustomsPercent() != null) {
            entity.setTaxCustomsPercent(request.getTaxCustomsPercent());
        }
        if (request.getShippingFee() != null) {
            entity.setShippingFee(request.getShippingFee());
        }
        if (request.getAvgLaborCostPerPiece() != null) {
            entity.setAvgLaborCostPerPiece(request.getAvgLaborCostPerPiece());
        }
        if (request.getAvgProductCost() != null) {
            entity.setAvgProductCost(request.getAvgProductCost());
        }
        if (request.getProductionLeadTimeDays() != null) {
            entity.setProductionLeadTimeDays(request.getProductionLeadTimeDays());
        }
        if (request.getVendorType() != null) {
            entity.setVendorType(request.getVendorType());
        }
        if (request.getLaborCostFactors() != null) {
            entity.setLaborCostFactors(request.getLaborCostFactors());
        }
        if (request.getContactPerson() != null) {
            entity.setContactPerson(request.getContactPerson());
        }
        if (request.getContactEmail() != null) {
            entity.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            entity.setContactPhone(request.getContactPhone());
        }
        if (request.getAddress() != null) {
            entity.setAddress(request.getAddress());
        }
    }
}