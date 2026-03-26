package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.CertificateType;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertificateResponse {

    private String id;
    private String certificateCode;
    private CertificateType certificateType;
    private String certificateUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
