package com.mirror.product.dto.pod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WholesaleOrderCreateRequest {

    @NotEmpty(message = "At least one item is required")
    @Size(max = 50, message = "Cannot have more than 50 items per order")
    @Valid
    private List<WholesaleOrderItemRequest> items;

    @Size(max = 1000, message = "Shipping address must not exceed 1000 characters")
    private String shippingAddress;

    @Size(max = 50, message = "Shipping method must not exceed 50 characters")
    private String shippingMethod;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
