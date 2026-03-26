package com.mirror.product.dto.pod;

import com.mirror.product.enums.PartnerStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private PartnerStatus status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
