package com.mirror.product.dto.pod;

import com.mirror.product.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanSearchCriteria {

    private String qrCodeId;
    private String podId;
    private String productId;
    private String partnerId;
    private String sessionId;
    private Long userId;
    private DeviceType deviceType;
    private String country;
    private String city;
    private Instant scannedAfter;
    private Instant scannedBefore;
    private Boolean uniqueOnly;
}
