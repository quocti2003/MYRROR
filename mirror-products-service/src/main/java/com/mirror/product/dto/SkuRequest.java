package com.mirror.product.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuRequest {

    @NotBlank(message = "SKU name cannot be blank")
    @Size(max = 255, message = "SKU name must be less than 255 characters")
    private String skuName;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    @Size(max = 255, message = "SKU code must be less than 255 characters")
    private String skuCode;
}
