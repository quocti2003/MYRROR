package com.mirror.product.dto.jtrc;

import com.mirror.product.enums.JTRCStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating JTRC status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JTRCStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private JTRCStatus status;

    // Optional reason for status change (for audit trail)
    private String reason;
}
