package com.mirror.product.dto.label;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RFIDScanRequest {

    @NotBlank(message = "EPC or code is required")
    private String epc;

    private String code; // Alternative: barcode or SKU

    private String deviceId;

    private String deviceName;

    private String deviceType;

    private String location;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String userId;

    private String sessionId;

    private String scanType; // BARCODE, QR_CODE, RFID
}
