package com.mirror.product.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemVariantRequest {
    
    @NotBlank(message = "Item variant URL cannot be blank")
    @Size(max = 500, message = "Item variant URL must be less than 500 characters")
    private String itemVariantUrl;
    
    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;
}