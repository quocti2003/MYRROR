package com.mirror.product.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComponentOptionalRequest {
    
    @NotBlank(message = "Component optional name cannot be blank")
    @Size(max = 255, message = "Component optional name must be less than 255 characters")
    private String componentOptionalName;
    
    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;
    
    @NotBlank(message = "Component ID cannot be blank")
    private String componentId;
}