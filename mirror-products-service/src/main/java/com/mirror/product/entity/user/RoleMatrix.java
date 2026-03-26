package com.mirror.product.entity.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_matrix", indexes = {
        @Index(name = "idx_role_matrix_role_name", columnList = "role_name")
})
@Data
@ToString
@EqualsAndHashCode
public class RoleMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", unique = true, nullable = false, length = 50)
    private String roleName;

    @Column(name = "purpose", nullable = false, length = 255)
    private String purpose;

    @Column(name = "ui_tabs", columnDefinition = "TEXT")
    private String uiTabs;

    @Column(name = "key_actions", columnDefinition = "TEXT")
    private String keyActions;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
