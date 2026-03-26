package com.mirror.product.entity.misa;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "misa_product_categories")
@Data
@EqualsAndHashCode(callSuper = false)
public class MisaProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", unique = true, nullable = false)
    private String categoryId;

    @Column(name = "category_code", nullable = false)
    private String categoryCode;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "parent_id")
    private String parentId;

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "is_inactive")
    private Boolean isInactive = false;

    @Column(name = "is_leaf")
    private Boolean isLeaf = false;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "full_path")
    private String fullPath;

    @Column(name = "level_names")
    private String levelNames;

    @Column(name = "misa_last_modified")
    private LocalDateTime misaLastModified;

    @Column(name = "last_sync_date")
    private LocalDateTime lastSyncDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "sync_status")
    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus = SyncStatus.PENDING;

    @Column(name = "sync_error_message", columnDefinition = "TEXT")
    private String syncErrorMessage;

    public enum SyncStatus {
        PENDING,
        SYNCED,
        ERROR,
        UPDATED
    }

    /**
     * Helper method to check if this is a root category
     */
    public boolean isRootCategory() {
        return parentId == null || parentId.trim().isEmpty();
    }

    /**
     * Helper method to check if this category is active
     */
    public boolean isActive() {
        return !Boolean.TRUE.equals(isInactive);
    }
}
