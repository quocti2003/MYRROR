package com.mirror.product.dto.productops;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDraftRequest {
    @NotBlank(message = "Product name is required")
    @Size(max = 500)
    private String name;

    @NotBlank(message = "Internal SKU is required")
    private String internalSKU;

    @NotBlank(message = "Category is required")
    private String category;

    private String collection;
}
