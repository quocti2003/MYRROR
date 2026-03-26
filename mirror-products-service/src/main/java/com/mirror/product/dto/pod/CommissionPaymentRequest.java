package com.mirror.product.dto.pod;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to mark a commission as paid.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionPaymentRequest {

    @NotBlank(message = "Payment reference is required")
    private String paymentReference;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    /**
     * Optional notes about the payment.
     */
    private String notes;
}
