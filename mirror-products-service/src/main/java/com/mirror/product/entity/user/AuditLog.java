package com.mirror.product.entity.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "username"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_ip", columnList = "ipAddress")
})
@Data
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 50)
    @Size(max = 50, message = "Username cannot exceed 50 characters")
    private String username;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Action is required")
    @Size(max = 100, message = "Action cannot exceed 100 characters")
    private String action;
    
    @Column(length = 255)
    @Size(max = 255, message = "Resource cannot exceed 255 characters")
    private String resource;
    
    @Column(name = "ip_address", length = 45)
    @Size(max = 45, message = "IP address cannot exceed 45 characters")
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    @Size(max = 500, message = "User agent cannot exceed 500 characters")
    private String userAgent;
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    @Column(nullable = false)
    private Boolean success;
    
    @Column(name = "error_message", length = 500)
    @Size(max = 500, message = "Error message cannot exceed 500 characters")
    private String errorMessage;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "session_id", length = 100)
    @Size(max = 100, message = "Session ID cannot exceed 100 characters")
    private String sessionId;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}