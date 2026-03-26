package com.mirror.product.dto.productops;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MISAProductResponse {
    private String id;
    private String name;
    private String internalSKU;
    private String misaSKU;
    private String thumbnail;
    private String category;

    private String misaStatus; // PENDING, SYNCING, SYNCED, FAILED

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime misaRequestedAt;

    private String misaRequestedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime misaSyncedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime misaLastAttempt;

    private Integer misaAttempts;
    private String misaError;
}
