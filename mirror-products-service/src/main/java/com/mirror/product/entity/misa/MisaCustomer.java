package com.mirror.product.entity.misa;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "misa_customers")
@Data
@EqualsAndHashCode(callSuper = false)
public class MisaCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", unique = true, nullable = false)
    private String customerId;

    @Column(name = "customer_code")
    private String customerCode;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "normalized_phone")
    private String normalizedPhone;

    @Column(name = "email")
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "province")
    private String province;

    @Column(name = "district")
    private String district;

    @Column(name = "commune")
    private String commune;

    @Column(name = "gender")
    private Integer gender;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "identify_number")
    private String identifyNumber;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "membership_code")
    private String membershipCode;

    @Column(name = "member_level_id")
    private String memberLevelId;

    @Column(name = "member_level_name")
    private String memberLevelName;

    @Column(name = "customer_category_id")
    private String customerCategoryId;

    @Column(name = "customer_category_name")
    private String customerCategoryName;

    @Column(name = "is_active")
    private Boolean isActive = true;

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
}
