package com.mirror.product.entity.pod;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.entity.BaseEntity;
import com.mirror.product.entity.user.User;
import com.mirror.product.enums.BusinessType;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.enums.PartnerStatus;
import com.mirror.product.enums.PartnerTier;
import com.mirror.product.enums.PartnerType;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a POD Partner.
 * Partners are businesses (spas, hotels, salons, etc.) that host POD display units.
 */
@Entity
@Table(name = "pod_partners", indexes = {
    @Index(name = "idx_pod_partner_status", columnList = "status"),
    @Index(name = "idx_pod_partner_tier", columnList = "tier"),
    @Index(name = "idx_pod_partner_business_type", columnList = "business_type")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PodPartner extends BaseEntity {

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 50)
    private BusinessType businessType;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "contact_email", nullable = false, unique = true)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "business_license", length = 100)
    private String businessLicense;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 100)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PartnerStatus status = PartnerStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    @Builder.Default
    private PartnerTier tier = PartnerTier.BRONZE;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("5.00");

    // === FRANCHISE/PHYGITAL FIELDS ===

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", nullable = false, length = 20)
    @Builder.Default
    private PartnerType partnerType = PartnerType.LOCATION;

    @Column(name = "wholesale_discount_rate", precision = 5, scale = 2)
    private BigDecimal wholesaleDiscountRate;

    @Column(name = "territory")
    private String territory;

    @Column(name = "can_set_own_prices", nullable = false)
    @Builder.Default
    private Boolean canSetOwnPrices = false;

    @Column(name = "min_markup_percent", precision = 5, scale = 2)
    private BigDecimal minMarkupPercent;

    @Column(name = "max_markup_percent", precision = 5, scale = 2)
    private BigDecimal maxMarkupPercent;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "security_deposit", precision = 15, scale = 2)
    private BigDecimal securityDeposit;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Pod> pods = new ArrayList<>();

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PodCommission> commissions = new ArrayList<>();

    @OneToMany(mappedBy = "partner", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PartnerInventory> inventoryItems = new ArrayList<>();

    @OneToMany(mappedBy = "partner", fetch = FetchType.LAZY)
    @Builder.Default
    private List<WholesaleOrder> wholesaleOrders = new ArrayList<>();

    @OneToMany(mappedBy = "partner", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PartnerSale> sales = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.PTN));
        }
    }

    public boolean isPhygitalPartner() {
        return PartnerType.PHYGITAL.equals(this.partnerType);
    }

    public boolean isLocationPartner() {
        return PartnerType.LOCATION.equals(this.partnerType);
    }

    public BigDecimal getWholesalePrice(BigDecimal retailPrice) {
        if (wholesaleDiscountRate == null || retailPrice == null) return retailPrice;
        BigDecimal discount = retailPrice.multiply(wholesaleDiscountRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return retailPrice.subtract(discount);
    }

    /**
     * Get full address as a formatted string
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressLine1 != null) sb.append(addressLine1);
        if (addressLine2 != null) sb.append(", ").append(addressLine2);
        if (city != null) sb.append(", ").append(city);
        if (state != null) sb.append(", ").append(state);
        if (postalCode != null) sb.append(" ").append(postalCode);
        if (country != null) sb.append(", ").append(country);
        return sb.toString();
    }
}
