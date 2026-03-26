package com.mirror.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionProductRequest {
    
    @NotBlank(message = "Collection ID is required")
    private String collectionId;
    
    @NotBlank(message = "Product ID is required")
    private String productId;
    
    private Integer sortOrder = 0;
    
    private Boolean isHeroProduct = false;
}