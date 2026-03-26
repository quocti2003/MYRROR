package com.mirror.product.dto.pod;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeCreateRequest {

    @NotBlank(message = "POD ID is required")
    private String podId;

    @NotBlank(message = "Product ID is required")
    private String productId;

    /**
     * Optional custom short code. If not provided, one will be generated.
     */
    private String customShortCode;

    /**
     * Optional expiration date for the QR code.
     */
    private Instant expiresAt;
}
