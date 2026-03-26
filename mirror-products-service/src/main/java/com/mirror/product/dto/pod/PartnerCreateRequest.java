package com.mirror.product.dto.pod;

import com.mirror.product.enums.BusinessType;
import com.mirror.product.enums.PartnerTier;
import com.mirror.product.enums.PartnerType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerCreateRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 255, message = "Business name must not exceed 255 characters")
    private String businessName;

    @NotNull(message = "Business type is required")
    private BusinessType businessType;

    @NotBlank(message = "Contact name is required")
    @Size(max = 100, message = "Contact name must not exceed 100 characters")
    private String contactName;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Contact email must be valid")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    private String contactEmail;

    @Size(max = 50, message = "Contact phone must not exceed 50 characters")
    private String contactPhone;

    @Size(max = 100, message = "Business license must not exceed 100 characters")
    private String businessLicense;

    @Size(max = 50, message = "Tax ID must not exceed 50 characters")
    private String taxId;

    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String addressLine2;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    private PartnerTier tier;

    @DecimalMin(value = "0.0", message = "Commission rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Commission rate must not exceed 100")
    private BigDecimal commissionRate;

    // === PARTNER TYPE & PHYGITAL FIELDS ===

    private PartnerType partnerType;

    @DecimalMin(value = "0.0", message = "Wholesale discount rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Wholesale discount rate must not exceed 100")
    private BigDecimal wholesaleDiscountRate;

    @Size(max = 255, message = "Territory must not exceed 255 characters")
    private String territory;

    private Boolean canSetOwnPrices;

    @DecimalMin(value = "0.0", message = "Min markup percent must be non-negative")
    @DecimalMax(value = "100.0", message = "Min markup percent must not exceed 100")
    private BigDecimal minMarkupPercent;

    @DecimalMin(value = "0.0", message = "Max markup percent must be non-negative")
    @DecimalMax(value = "100.0", message = "Max markup percent must not exceed 100")
    private BigDecimal maxMarkupPercent;

    private LocalDate contractStartDate;

    private LocalDate contractEndDate;

    @DecimalMin(value = "0.0", message = "Security deposit must be non-negative")
    private BigDecimal securityDeposit;

    @Size(max = 50, message = "Bank account number must not exceed 50 characters")
    private String bankAccountNumber;

    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    @Size(max = 100, message = "Bank branch must not exceed 100 characters")
    private String bankBranch;

    private String notes;
}
