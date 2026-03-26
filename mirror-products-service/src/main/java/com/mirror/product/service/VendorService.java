package com.mirror.product.service;

import com.mirror.product.entity.Vendor;
import com.mirror.product.enums.VendorType;
import com.mirror.product.repository.VendorRepository;
import com.mirror.product.repository.VendorProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VendorService extends BaseService<Vendor, String> {
    
    private final VendorRepository vendorRepository;
    private final VendorProductRepository vendorProductRepository;
    
    public VendorService(VendorRepository vendorRepository, VendorProductRepository vendorProductRepository) {
        super(vendorRepository);
        this.vendorRepository = vendorRepository;
        this.vendorProductRepository = vendorProductRepository;
    }
    
    public Optional<Vendor> findByCode(String code) {
        return vendorRepository.findActiveByCode(code);
    }
    
    public boolean existsByCode(String code) {
        return vendorRepository.existsActiveByCode(code);
    }
    
    public List<Vendor> findByCountry(String country) {
        return vendorRepository.findActiveByCountry(country);
    }
    
    public List<Vendor> findByVendorType(VendorType vendorType) {
        return vendorRepository.findActiveByVendorType(vendorType);
    }
    
    public List<Vendor> findByOwnerUserId(String ownerUserId) {
        return vendorRepository.findActiveByOwnerUserId(ownerUserId);
    }
    
    public Page<Vendor> searchVendors(String search, Pageable pageable) {
        return vendorRepository.searchActiveVendors(search, pageable);
    }
    
    @Override
    public Vendor update(String vendorId, Vendor vendorDetails) {
        Vendor vendor = findActiveById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + vendorId));

        // Validate unique vendor code for update
        if (!vendor.getCode().equals(vendorDetails.getCode()) &&
            vendorRepository.existsActiveByCodeAndNotId(vendorDetails.getCode(), vendorId)) {
            throw new IllegalArgumentException("Vendor code already exists: " + vendorDetails.getCode());
        }

        vendor.setCode(vendorDetails.getCode());
        vendor.setName(vendorDetails.getName());
        vendor.setCountry(vendorDetails.getCountry());
        vendor.setCountryOfOrigin(vendorDetails.getCountryOfOrigin());
        vendor.setPaymentTerms(vendorDetails.getPaymentTerms());
        vendor.setCommissionTerm(vendorDetails.getCommissionTerm());
        vendor.setImportTaxPercent(vendorDetails.getImportTaxPercent());
        vendor.setVatTaxPercent(vendorDetails.getVatTaxPercent());
        vendor.setTaxCustomsPercent(vendorDetails.getTaxCustomsPercent());
        vendor.setShippingFee(vendorDetails.getShippingFee());
        vendor.setAvgLaborCostPerPiece(vendorDetails.getAvgLaborCostPerPiece());
        vendor.setAvgProductCost(vendorDetails.getAvgProductCost());
        vendor.setProductionLeadTimeDays(vendorDetails.getProductionLeadTimeDays());
        vendor.setVendorType(vendorDetails.getVendorType());
        vendor.setLaborCostFactors(vendorDetails.getLaborCostFactors());
        vendor.setContactPerson(vendorDetails.getContactPerson());
        vendor.setContactEmail(vendorDetails.getContactEmail());
        vendor.setContactPhone(vendorDetails.getContactPhone());
        vendor.setAddress(vendorDetails.getAddress());

        return save(vendor);
    }

    @Override
    protected void validateBeforeSave(Vendor vendor) {
        if (vendor.getId() == null || vendor.getId().isEmpty()) {
            // Only validate for new vendors (no ID)
            if (vendorRepository.existsActiveByCode(vendor.getCode())) {
                throw new IllegalArgumentException("Vendor code already exists: " + vendor.getCode());
            }
        }
    }

    @Override
    public Vendor save(Vendor vendor) {
        if (vendor.getId() == null || vendor.getId().isEmpty()) {
            validateBeforeSave(vendor);
        } else {
            // For updates, validate only if vendor code changed
            Vendor existing = findActiveById(vendor.getId()).orElse(null);
            if (existing != null && !existing.getCode().equals(vendor.getCode())) {
                if (vendorRepository.existsActiveByCodeAndNotId(vendor.getCode(), vendor.getId())) {
                    throw new IllegalArgumentException("Vendor code already exists: " + vendor.getCode());
                }
            }
        }
        return super.save(vendor);
    }
    
    @Override
    protected void validateBeforeDelete(String vendorId) {
        // Check if vendor has any products
        long productCount = vendorProductRepository.countByVendorId(vendorId);
        if (productCount > 0) {
            throw new IllegalStateException("Cannot delete vendor. There are " + productCount + " products associated with this vendor.");
        }
    }
}