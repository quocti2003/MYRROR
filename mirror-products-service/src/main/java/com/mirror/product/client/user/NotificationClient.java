package com.mirror.product.client.user;

import com.mirror.product.dto.notification.event.NotificationEvent;
import com.mirror.product.entity.user.User;
import com.mirror.product.service.notification.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Client for sending user-related notification emails.
 * Uses local NotificationDispatcher instead of HTTP calls (monolith architecture).
 */
@Service
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final NotificationDispatcher notificationDispatcher;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.frontend.verification-path:/verify-email}")
    private String verificationPath;

    public NotificationClient(NotificationDispatcher notificationDispatcher) {
        this.notificationDispatcher = notificationDispatcher;
    }

    @Async
    public CompletableFuture<Void> sendEmailVerification(User user, String token) {
        try {
            String verificationUrl = buildVerificationUrl(token);

            Map<String, Object> payload = new HashMap<>();
            payload.put("recipients", List.of(user.getEmail()));
            payload.put("username", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
            payload.put("verificationUrl", verificationUrl);

            NotificationEvent event = new NotificationEvent(
                    "user.email.verification",
                    user.getId().toString(),
                    Instant.now(),
                    payload
            );

            notificationDispatcher.dispatch(event);

            log.info("Verification email sent to user={} email={}", user.getId(), user.getEmail());
        } catch (Exception e) {
            // Log but don't throw - don't fail registration if email fails
            log.error("Failed to send verification email for user={}", user.getId(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> sendPasswordResetOtp(User user, String otp) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("recipients", List.of(user.getEmail()));
            payload.put("username", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
            payload.put("otp", otp);

            NotificationEvent event = new NotificationEvent(
                    "user.password.reset",
                    user.getId().toString(),
                    Instant.now(),
                    payload
            );

            notificationDispatcher.dispatch(event);

            log.info("Password reset OTP sent to user={} email={}", user.getId(), user.getEmail());
        } catch (Exception e) {
            // Log but don't throw - don't fail password reset flow if email fails
            log.error("Failed to send password reset OTP for user={}", user.getId(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private String buildVerificationUrl(String token) {
        // Build URL: https://mirror.com/verify-email?token=xxx
        String baseUrl = frontendUrl.endsWith("/")
            ? frontendUrl.substring(0, frontendUrl.length() - 1)
            : frontendUrl;
        String path = verificationPath.startsWith("/")
            ? verificationPath
            : "/" + verificationPath;
        return baseUrl + path + "?token=" + token;
    }
}
