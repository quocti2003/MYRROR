package com.mirror.product.dto.pod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeBatchCreateRequest {

    @NotBlank(message = "POD ID is required")
    private String podId;

    @NotEmpty(message = "At least one product ID is required")
    private Set<String> productIds;

    /**
     * Optional expiration date for all QR codes in batch.
     */
    private Instant expiresAt;

    /**
     * If true, regenerate QR codes for products that already have one.
     */
    @Builder.Default
    private boolean regenerateExisting = false;
}
