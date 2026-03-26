package com.mirror.product.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MisaProductCategoryDto {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("ParentId")
    private String parentId;

    @JsonProperty("Grade")
    private Integer grade;

    @JsonProperty("Inactive")
    private Boolean inactive;

    @JsonProperty("IsLeaf")
    private Boolean isLeaf;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("SortOrder")
    private Integer sortOrder;

    @JsonProperty("FullPath")
    private String fullPath;

    @JsonProperty("LevelNames")
    private String levelNames;

    @JsonProperty("LastModified")
    private OffsetDateTime lastModified;
}
