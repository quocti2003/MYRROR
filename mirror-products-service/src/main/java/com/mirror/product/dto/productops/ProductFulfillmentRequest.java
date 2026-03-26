package com.mirror.product.dto.productops;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFulfillmentRequest {
    @NotBlank(message = "Description is required")
    @Size(min = 50)
    private String description;

    @NotBlank(message = "Short description is required")
    @Size(min = 20, max = 100)
    private String shortDescription;

    private List<String> imageUrls;

    @NotBlank(message = "Category is required")
    private String category;

    private String collection;
    private Map<String, String> specifications;
    private List<String> tags;
}
