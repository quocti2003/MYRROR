package com.mirror.product.dto;

import com.mirror.product.entity.CollectionProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionProductResponse {
    
    private String id;
    private String collectionId;
    private String productId;
    private Integer sortOrder;
    private Boolean isHeroProduct;
    private CollectionResponse collection;
    private ProductResponse product;
    private Instant createdAt;
    private Instant updatedAt;
    
    public CollectionProductResponse(CollectionProduct collectionProduct) {
        this.id = collectionProduct.getId();
        this.collectionId = collectionProduct.getCollectionId();
        this.productId = collectionProduct.getProductId();
        this.sortOrder = collectionProduct.getSortOrder();
        this.isHeroProduct = collectionProduct.getIsHeroProduct();
        this.createdAt = collectionProduct.getCreatedAt();
        this.updatedAt = collectionProduct.getUpdatedAt();
    }
}