package com.mirror.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderVendorAssignRequest {

    @NotBlank
    private String vendorId;
}
