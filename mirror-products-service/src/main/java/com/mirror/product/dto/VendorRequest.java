package com.mirror.product.dto;

import com.mirror.product.enums.VendorType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorRequest {
    
    @NotBlank(message = "Vendor code is required")
    @Size(max = 50, message = "Vendor code must not exceed 50 characters")
    private String code;
    
    @NotBlank(message = "Vendor name is required")
    @Size(max = 200, message = "Vendor name must not exceed 200 characters")
    private String name;
    
    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;
    
    @Size(max = 100, message = "Country of origin must not exceed 100 characters")
    private String countryOfOrigin;
    
    @Size(max = 200, message = "Payment terms must not exceed 200 characters")
    private String paymentTerms;
    
    private String commissionTerm;
    
    @DecimalMin(value = "0.0", message = "Import tax percent must be non-negative")
    @DecimalMax(value = "100.0", message = "Import tax percent must not exceed 100")
    private BigDecimal importTaxPercent;
    
    @DecimalMin(value = "0.0", message = "VAT tax percent must be non-negative")
    @DecimalMax(value = "100.0", message = "VAT tax percent must not exceed 100")
    private BigDecimal vatTaxPercent;
    
    @DecimalMin(value = "0.0", message = "Tax customs percent must be non-negative")
    @DecimalMax(value = "100.0", message = "Tax customs percent must not exceed 100")
    private BigDecimal taxCustomsPercent;
    
    @DecimalMin(value = "0.0", message = "Shipping fee must be non-negative")
    private BigDecimal shippingFee;
    
    @DecimalMin(value = "0.0", message = "Average labor cost must be non-negative")
    private BigDecimal avgLaborCostPerPiece;
    
    @DecimalMin(value = "0.0", message = "Average product cost must be non-negative")
    private BigDecimal avgProductCost;
    
    @Min(value = 0, message = "Production lead time must be non-negative")
    private Integer productionLeadTimeDays;
    
    private VendorType vendorType;
    
    private String laborCostFactors;
    
    @Size(max = 200, message = "Contact person must not exceed 200 characters")
    private String contactPerson;
    
    @Email(message = "Contact email must be valid")
    @Size(max = 200, message = "Contact email must not exceed 200 characters")
    private String contactEmail;
    
    @Size(max = 50, message = "Contact phone must not exceed 50 characters")
    private String contactPhone;
    
    private String address;
}