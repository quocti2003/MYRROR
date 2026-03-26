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
public class ComponentResponse {

    private String id;
    private String componentName;
    private String description;
    private String productId;
    private String productName;
    private String productDescription;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isActive;
}
