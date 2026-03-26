package com.mirror.product.dto.pod;

import com.mirror.product.enums.BusinessType;
import com.mirror.product.enums.PartnerStatus;
import com.mirror.product.enums.PartnerTier;
import com.mirror.product.enums.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerSearchCriteria {

    private String keyword;
    private PartnerStatus status;
    private PartnerTier tier;
    private BusinessType businessType;
    private PartnerType partnerType;
    private String city;
    private String country;
}
