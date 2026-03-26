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
public class MISAHistoryResponse {
    private String id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private Instant timestamp;

    private String action; // SYNC_REQUESTED, SYNC_STARTED, SYNC_SUCCESS, SYNC_FAILED
    private String user;
    private String status;
    private String message;
}
