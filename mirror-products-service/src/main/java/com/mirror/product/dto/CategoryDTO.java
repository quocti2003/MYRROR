package com.mirror.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long id;
    private String categoryId;
    private String categoryName;
    private String categoryCode;
    private String parentId;
    private Integer grade;
    private Boolean isLeaf;
    private Boolean isActive;
    private Boolean isInactive;
    private String description;
    private Integer sortOrder;
    private String fullPath;
    private String levelNames;
    private LocalDateTime misaLastModified;
    private LocalDateTime lastSyncDate;
    private String syncStatus;
    private String syncErrorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
