package com.mirror.product.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComponentRequest {

    @NotBlank(message = "Component name cannot be blank")
    @Size(max = 255, message = "Component name must be less than 255 characters")
    private String componentName;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    @NotBlank(message = "Product ID cannot be blank")
    private String productId;
}
