package com.mirror.product.service.user;

import com.mirror.product.client.user.NotificationClient;
import com.mirror.product.entity.user.EmailVerificationToken;
import com.mirror.product.entity.user.User;
import com.mirror.product.repository.user.EmailVerificationTokenRepository;
import com.mirror.product.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final String TOKEN_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 64;

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final NotificationClient notificationClient;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom;

    @Value("${app.email-verification.token-expiration:86400}")
    private long tokenExpirationSeconds;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            NotificationClient notificationClient,
            AuditLogService auditLogService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.notificationClient = notificationClient;
        this.auditLogService = auditLogService;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public void createAndSendVerificationToken(User user) {
        // Invalidate any existing pending tokens
        tokenRepository.invalidateAllPendingTokensByUserId(user.getId());

        // Generate secure token
        String token = generateSecureToken();

        // Calculate expiration
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenExpirationSeconds);

        // Create and save token
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        tokenRepository.save(verificationToken);

        // Send verification email asynchronously
        notificationClient.sendEmailVerification(user, token);

        // Audit log
        auditLogService.logUserEvent(
                user.getUsername(),
                "EMAIL_VERIFICATION_SENT",
                "EmailVerificationToken",
                "Created verification token for user",
                null,
                true,
                null,
                null
        );

        log.info("Created verification token for user={} expiresAt={}", user.getId(), expiresAt);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (verificationToken.isVerified()) {
            // Already verified - idempotent
            log.info("Token already verified for user={}", verificationToken.getUser().getId());
            return;
        }

        if (verificationToken.isExpired()) {
            auditLogService.logUserEvent(
                    verificationToken.getUser().getUsername(),
                    "EMAIL_VERIFICATION_FAILED",
                    "EmailVerificationToken",
                    "Token expired",
                    null,
                    false,
                    "Token has expired",
                    null
            );
            throw new IllegalArgumentException("Verification token has expired");
        }

        // Mark token as verified
        verificationToken.markAsVerified();
        tokenRepository.save(verificationToken);

        // Mark user as verified
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        // Audit log
        auditLogService.logUserEvent(
                user.getUsername(),
                "EMAIL_VERIFIED",
                "User",
                "Email verified successfully",
                null,
                true,
                null,
                null
        );

        log.info("Email verified for user={}", user.getId());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getEmailVerified()) {
            throw new IllegalStateException("Email already verified");
        }

        // Create and send new verification token
        createAndSendVerificationToken(user);

        log.info("Resent verification email to user={}", user.getId());
    }

    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired verification tokens", deleted);
        }
    }

    private String generateSecureToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(TOKEN_CHARACTERS.length());
            token.append(TOKEN_CHARACTERS.charAt(randomIndex));
        }
        return token.toString();
    }
}
