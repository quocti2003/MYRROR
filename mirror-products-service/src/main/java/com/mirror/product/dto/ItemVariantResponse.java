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
public class ItemVariantResponse {
    
    private String id;
    private String itemVariantUrl;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isActive;
}