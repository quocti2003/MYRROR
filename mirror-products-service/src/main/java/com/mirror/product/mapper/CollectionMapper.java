package com.mirror.product.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.dto.CollectionRequest;
import com.mirror.product.dto.CollectionResponse;
import com.mirror.product.entity.Collection;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CollectionMapper {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Convert CollectionRequest to Collection entity for creation
     */
    public Collection toEntity(CollectionRequest request) {
        if (request == null) {
            return null;
        }
        
        Collection collection = new Collection();
        collection.setName(request.getName());
        collection.setTitle(request.getTitle());
        collection.setDescription(request.getDescription());
        collection.setSeason(request.getSeason());
        collection.setYear(request.getYear());
        collection.setTheme(request.getTheme());
        collection.setImageUrl(request.getImageUrl());
        collection.setBannerImageUrl(request.getBannerImageUrl());
        collection.setImageUrls(request.getImageUrls());
        collection.setStatus(request.getStatus() != null ? request.getStatus() : Collection.CollectionStatus.ACTIVE);
        collection.setFeatured(request.getFeatured() != null ? request.getFeatured() : false);
        collection.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        collection.setStartDate(request.getStartDate());
        collection.setEndDate(request.getEndDate());
        
        return collection;
    }
    
    /**
     * Update existing Collection entity with CollectionRequest data
     */
    public void updateEntity(Collection existing, CollectionRequest request) {
        if (existing == null || request == null) {
            return;
        }
        
        existing.setName(request.getName());
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setSeason(request.getSeason());
        existing.setYear(request.getYear());
        existing.setTheme(request.getTheme());
        existing.setImageUrl(request.getImageUrl());
        existing.setBannerImageUrl(request.getBannerImageUrl());
        existing.setImageUrls(request.getImageUrls());
        
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (request.getFeatured() != null) {
            existing.setFeatured(request.getFeatured());
        }
        if (request.getSortOrder() != null) {
            existing.setSortOrder(request.getSortOrder());
        }
        
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
    }
    
    /**
     * Convert Collection entity to CollectionResponse
     */
    public CollectionResponse toResponse(Collection collection) {
        if (collection == null) {
            return null;
        }
        
        CollectionResponse response = new CollectionResponse(collection);
        
        // Parse JSON imageUrls if available
        if (collection.getImageUrls() != null) {
            response.setImageUrls(parseJsonStringList(collection.getImageUrls()));
        }
        
        return response;
    }
    
    /**
     * Convert list of Collection entities to list of CollectionResponse
     */
    public List<CollectionResponse> toResponseList(List<Collection> collections) {
        if (collections == null) {
            return null;
        }
        
        return collections.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert Collection entity to simplified response for listings
     */
    public CollectionResponse toSimpleResponse(Collection collection) {
        if (collection == null) {
            return null;
        }
        
        CollectionResponse response = CollectionResponse.builder()
                .id(collection.getId())
                .name(collection.getName())
                .title(collection.getTitle())
                .imageUrl(collection.getImageUrl())
                .bannerImageUrl(collection.getBannerImageUrl())
                .status(collection.getStatus())
                .featured(collection.getFeatured())
                .sortOrder(collection.getSortOrder())
                .startDate(collection.getStartDate())
                .endDate(collection.getEndDate())
                .isActive(collection.isActive())
                .isLaunched(collection.isLaunched())
                .isEnded(collection.isEnded())
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .build();
        
        return response;
    }
    
    /**
     * Convert list of Collection entities to simplified response list
     */
    public List<CollectionResponse> toSimpleResponseList(List<Collection> collections) {
        if (collections == null) {
            return null;
        }
        
        return collections.stream()
                .map(this::toSimpleResponse)
                .collect(Collectors.toList());
    }
    
    // Helper methods for JSON conversion
    private List<String> parseJsonStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}