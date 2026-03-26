package com.mirror.product.dto.label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RFIDTagRegisterRequest {

    @NotBlank(message = "EPC is required")
    @Pattern(regexp = "^[A-Fa-f0-9]{24}$", message = "EPC must be 24 hex characters")
    private String epc;

    @NotBlank(message = "Product ID is required")
    private String productId;

    private String printJobId;

    private String printJobItemId;

    private Map<String, Object> metadata;
}
