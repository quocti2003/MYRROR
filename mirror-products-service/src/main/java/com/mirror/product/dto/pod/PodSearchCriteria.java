package com.mirror.product.dto.pod;

import com.mirror.product.enums.PodStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodSearchCriteria {

    private String keyword;
    private String partnerId;
    private PodStatus status;
    private String city;
    private String country;
    private String productId;
}
