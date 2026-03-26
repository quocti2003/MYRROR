package com.mirror.product.dto.productops;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSummaryResponse {
    private Integer draft;
    private Integer inProgress;
    private Integer ready;
    private Integer published;
    private Integer total;
}
