package com.mirror.product.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentOptionalResponse {
    
    private String id;
    private String componentOptionalName;
    private String description;
    private String componentId;
    private String componentName;
    private String componentDescription;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isActive;
}