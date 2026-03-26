package com.mirror.product.dto.componentownership;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for confirming receipt of a component handoff
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmReceiptRequest {

    /**
     * Notes about the received items (e.g., condition, quantity verified)
     */
    private String notes;
}
