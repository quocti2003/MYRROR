package com.mirror.product.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkuResponse {

    private String id;
    private String skuName;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isActive;
    private String skuCode;
    private String misaCategoryId;
    private String misaCategoryCode;
    private String misaCategoryName;
    private Instant misaLastSyncedAt;
    private String misaInventoryId;
    private Boolean misaSynced;
}
