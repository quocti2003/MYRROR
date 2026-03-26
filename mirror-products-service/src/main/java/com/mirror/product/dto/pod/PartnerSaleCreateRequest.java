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
public class PartnerSaleCreateRequest {

    @NotEmpty(message = "At least one item is required")
    @Size(max = 50, message = "Cannot have more than 50 items per sale")
    @Valid
    private List<PartnerSaleItemRequest> items;

    private String podId;

    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    private String customerName;

    @Size(max = 20, message = "Customer phone must not exceed 20 characters")
    private String customerPhone;

    @Size(max = 255, message = "Customer email must not exceed 255 characters")
    private String customerEmail;

    @Size(max = 50, message = "Payment method must not exceed 50 characters")
    private String paymentMethod;

    @Size(max = 100, message = "Payment reference must not exceed 100 characters")
    private String paymentReference;

    private String qrCodeId;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
