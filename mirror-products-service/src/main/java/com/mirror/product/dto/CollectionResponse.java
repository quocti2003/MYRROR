package com.mirror.product.dto;

import com.mirror.product.entity.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionResponse {
    
    private String id;
    private String name;
    private String title;
    private String description;
    private String season;
    private Integer year;
    private String theme;
    private String imageUrl;
    private String bannerImageUrl;
    private List<String> imageUrls;
    private Collection.CollectionStatus status;
    private Boolean featured;
    private Integer sortOrder;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;
    private boolean isLaunched;
    private boolean isEnded;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer productCount;
    private List<ProductResponse> featuredProducts;
    private List<CollectionProductResponse> products;
    
    public CollectionResponse(Collection collection) {
        this.id = collection.getId();
        this.name = collection.getName();
        this.title = collection.getTitle();
        this.description = collection.getDescription();
        this.season = collection.getSeason();
        this.year = collection.getYear();
        this.theme = collection.getTheme();
        this.imageUrl = collection.getImageUrl();
        this.bannerImageUrl = collection.getBannerImageUrl();
        this.status = collection.getStatus();
        this.featured = collection.getFeatured();
        this.sortOrder = collection.getSortOrder();
        this.startDate = collection.getStartDate();
        this.endDate = collection.getEndDate();
        this.isActive = collection.isActive();
        this.isLaunched = collection.isLaunched();
        this.isEnded = collection.isEnded();
        this.createdAt = collection.getCreatedAt();
        this.updatedAt = collection.getUpdatedAt();
    }
}