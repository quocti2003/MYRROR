package com.mirror.product.dto;

import com.mirror.product.enums.CertificateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CertificateRequest {

    @NotBlank(message = "Certificate code is required")
    private String certificateCode;

    @NotNull(message = "Certificate type is required")
    private CertificateType certificateType;

    private String certificateUrl;
}
