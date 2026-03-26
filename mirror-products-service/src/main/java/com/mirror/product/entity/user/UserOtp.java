package com.mirror.product.entity.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_otps", indexes = {
    @Index(name = "idx_otp_email_type", columnList = "email,type,verified_at"),
    @Index(name = "idx_otp_user_pending", columnList = "user_id,type,verified_at"),
    @Index(name = "idx_otp_cleanup", columnList = "created_at,verified_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 6)
    private String otp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OtpType type;

    @Column(name = "expiration_seconds", nullable = false)
    private Long expirationSeconds;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        LocalDateTime expirationTime = createdAt.plusSeconds(expirationSeconds);
        return LocalDateTime.now().isAfter(expirationTime);
    }

    public LocalDateTime getExpiresAt() {
        return createdAt.plusSeconds(expirationSeconds);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public void markAsVerified() {
        this.verifiedAt = LocalDateTime.now();
    }

    public boolean hasExceededAttempts() {
        return attemptCount >= 3;
    }

    public void incrementAttemptCount() {
        this.attemptCount++;
    }
}
