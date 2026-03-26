package com.mirror.product.dto.pod;

import com.mirror.product.enums.QrCodeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeSearchCriteria {

    private String keyword;
    private String podId;
    private String productId;
    private String partnerId;
    private QrCodeStatus status;
    private Instant createdAfter;
    private Instant createdBefore;
    private Long minScanCount;
    private Long maxScanCount;
}
