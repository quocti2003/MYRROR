package com.mirror.product.dto.pod;

import com.mirror.product.enums.PodStatus;
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
public class PodStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private PodStatus status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
