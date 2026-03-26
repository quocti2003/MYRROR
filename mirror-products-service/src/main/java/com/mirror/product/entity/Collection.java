package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.mirror.product.util.SequenceIdGenerator;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.config.ApplicationContextProvider;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "collections")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Collection extends BaseEntity {

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "season", length = 50)
    private String season;

    @Column(name = "year")
    private Integer year;

    @Column(name = "theme", length = 200)
    private String theme;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "banner_image_url", length = 1000)
    private String bannerImageUrl;

    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls; // JSON array string

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CollectionStatus status = CollectionStatus.ACTIVE;

    @Column(name = "featured")
    private Boolean featured = false;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // Relationships
    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CollectionProduct> collectionProducts;

    // Helper methods
    public boolean isActive() {
        return status == CollectionStatus.ACTIVE;
    }

    public boolean isLaunched() {
        if (startDate == null) return false;
        return !LocalDate.now().isBefore(startDate);
    }

    public boolean isEnded() {
        if (endDate == null) return false;
        return LocalDate.now().isAfter(endDate);
    }


    // Enums
    public enum CollectionStatus {
        ACTIVE, INACTIVE, COMING_SOON, ENDED, ARCHIVED
    }

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.COL));
        }
    }

    @Override
    public String toString() {
        return "Collection{" +
                "id='" + getId() + '\'' +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", featured=" + featured +
                '}';
    }
}