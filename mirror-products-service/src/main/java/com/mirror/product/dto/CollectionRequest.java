package com.mirror.product.dto;

import com.mirror.product.entity.Collection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionRequest {
    
    @NotBlank(message = "Collection name is required")
    @Size(max = 500, message = "Name must not exceed 500 characters")
    private String name;
    
    @NotBlank(message = "Collection title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;
    
    private String description;
    
    @Size(max = 50, message = "Season must not exceed 50 characters")
    private String season;
    
    private Integer year;
    
    @Size(max = 200, message = "Theme must not exceed 200 characters")
    private String theme;
    
    @Size(max = 1000, message = "Image URL must not exceed 1000 characters")
    private String imageUrl;
    
    @Size(max = 1000, message = "Banner image URL must not exceed 1000 characters")
    private String bannerImageUrl;
    
    private String imageUrls; // JSON array string
    
    private Collection.CollectionStatus status = Collection.CollectionStatus.ACTIVE;
    
    private Boolean featured = false;
    
    private Integer sortOrder = 0;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
}