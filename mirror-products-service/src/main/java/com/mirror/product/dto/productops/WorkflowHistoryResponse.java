package com.mirror.product.dto.productops;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowHistoryResponse {
    private String id;
    private Integer stepId;
    private String stepName;
    private String action; // STEP_STARTED, STEP_COMPLETED, STEP_BLOCKED

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private Instant timestamp;

    private String user;
    private String notes;
}
