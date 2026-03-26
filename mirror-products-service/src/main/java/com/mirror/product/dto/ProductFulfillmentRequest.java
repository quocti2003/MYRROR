package com.mirror.product.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductFulfillmentRequest {

    private String description;

    private List<String> imageUrls;

    private List<String> assetUrls;

    @NotNull(message = "Product ID is required")
    private String productId;

    private Boolean markReadyForRelease;
}
