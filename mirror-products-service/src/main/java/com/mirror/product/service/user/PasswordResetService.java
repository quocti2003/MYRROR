package com.mirror.product.service.user;

import com.mirror.product.client.user.NotificationClient;
import com.mirror.product.dto.user.VerifyOtpResponse;
import com.mirror.product.entity.user.OtpType;
import com.mirror.product.entity.user.User;
import com.mirror.product.entity.user.UserOtp;
import com.mirror.product.repository.user.UserOtpRepository;
import com.mirror.product.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int OTP_LENGTH = 6;

    private final UserOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final NotificationClient notificationClient;
    private final AuditLogService auditLogService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    // Temporary storage for verified OTP sessions (email -> userId)
    // In production, consider using Redis for distributed systems
    private final Map<String, Long> verifiedOtpSessions = new HashMap<>();

    @Value("${app.password-reset.otp-expiration:300}")
    private long otpExpirationSeconds;

    @Value("${app.password-reset.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.password-reset.request-cooldown:60}")
    private long requestCooldownSeconds;

    @Value("${app.password-reset.reset-token-expiration:900}")
    private long resetTokenExpirationSeconds;

    public PasswordResetService(
            UserOtpRepository otpRepository,
            UserRepository userRepository,
            NotificationClient notificationClient,
            AuditLogService auditLogService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.notificationClient = notificationClient;
        this.auditLogService = auditLogService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check rate limiting
        checkRateLimit(email);

        // Invalidate any existing pending OTPs
        otpRepository.invalidateAllPendingOtpsByUserIdAndType(user.getId(), OtpType.FORGOT_PASSWORD);

        // Generate OTP
        String otp = generateOtp();

        // Create and save OTP
        UserOtp userOtp = UserOtp.builder()
                .user(user)
                .email(email)
                .otp(otp)
                .type(OtpType.FORGOT_PASSWORD)
                .expirationSeconds(otpExpirationSeconds)
                .attemptCount(0)
                .build();

        otpRepository.save(userOtp);

        // Send OTP email asynchronously
        notificationClient.sendPasswordResetOtp(user, otp);

        // Audit log
        auditLogService.logUserEvent(
                user.getUsername(),
                "PASSWORD_RESET_OTP_SENT",
                "UserOtp",
                "Password reset OTP sent to user",
                null,
                true,
                null,
                null
        );

        log.info("Password reset OTP created for user={} email={} expiresAt={}",
                user.getId(), email, userOtp.getExpiresAt());
    }

    @Transactional
    public VerifyOtpResponse verifyOtp(String email, String otpCode) {
        UserOtp userOtp = otpRepository.findByEmailAndTypeAndVerifiedAtIsNull(email, OtpType.FORGOT_PASSWORD)
                .orElseThrow(() -> new IllegalArgumentException("No pending OTP found for this email"));

        // Check if expired
        if (userOtp.isExpired()) {
            auditLogService.logUserEvent(
                    userOtp.getUser().getUsername(),
                    "PASSWORD_RESET_OTP_EXPIRED",
                    "UserOtp",
                    "OTP expired",
                    null,
                    false,
                    "OTP has expired",
                    null
            );
            throw new IllegalArgumentException("OTP has expired");
        }

        // Check if exceeded attempts
        if (userOtp.hasExceededAttempts()) {
            auditLogService.logUserEvent(
                    userOtp.getUser().getUsername(),
                    "PASSWORD_RESET_OTP_LOCKED",
                    "UserOtp",
                    "Too many failed attempts",
                    null,
                    false,
                    "Maximum attempts exceeded",
                    null
            );
            throw new IllegalArgumentException("Maximum attempts exceeded. Please request a new OTP.");
        }

        // Verify OTP
        if (!userOtp.getOtp().equals(otpCode)) {
            userOtp.incrementAttemptCount();
            otpRepository.save(userOtp);

            auditLogService.logUserEvent(
                    userOtp.getUser().getUsername(),
                    "PASSWORD_RESET_OTP_FAILED",
                    "UserOtp",
                    "Invalid OTP attempt " + userOtp.getAttemptCount() + "/" + maxAttempts,
                    null,
                    false,
                    "Invalid OTP",
                    null
            );

            int remainingAttempts = maxAttempts - userOtp.getAttemptCount();
            throw new IllegalArgumentException("Invalid OTP. " + remainingAttempts + " attempts remaining.");
        }

        // Mark OTP as verified
        userOtp.markAsVerified();
        otpRepository.save(userOtp);

        // Generate temporary reset token (JWT with short expiration)
        String resetToken = generateResetToken(userOtp.getUser());

        // Store verified session
        verifiedOtpSessions.put(resetToken, userOtp.getUser().getId());

        // Audit log
        auditLogService.logUserEvent(
                userOtp.getUser().getUsername(),
                "PASSWORD_RESET_OTP_VERIFIED",
                "UserOtp",
                "OTP verified successfully",
                null,
                true,
                null,
                null
        );

        log.info("OTP verified for user={} email={}", userOtp.getUser().getId(), email);

        return VerifyOtpResponse.builder()
                .resetToken(resetToken)
                .expiresIn(resetTokenExpirationSeconds)
                .message("OTP verified successfully. You can now reset your password.")
                .build();
    }

    @Transactional
    public void resendPasswordResetOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if there's a pending OTP
        Optional<UserOtp> existingOtp = otpRepository.findByEmailAndTypeAndVerifiedAtIsNull(email, OtpType.FORGOT_PASSWORD);

        // If OTP exists and not expired, resend the same OTP
        if (existingOtp.isPresent() && !existingOtp.get().isExpired()) {
            UserOtp otp = existingOtp.get();

            // Resend email with existing OTP
            notificationClient.sendPasswordResetOtp(user, otp.getOtp());

            // Audit log
            auditLogService.logUserEvent(
                    user.getUsername(),
                    "PASSWORD_RESET_OTP_RESENT",
                    "UserOtp",
                    "Password reset OTP resent to user",
                    null,
                    true,
                    null,
                    null
            );

            log.info("Password reset OTP resent for user={} email={}", user.getId(), email);
            return;
        }

        // If no OTP or expired, create new one (same as requestPasswordReset but with different rate limiting)
        // Check resend rate limiting (30 seconds instead of 60)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownTime = now.minusSeconds(30);

        // Only count pending (unverified and unexpired) OTPs for rate limiting
        int recentRequests = otpRepository.countPendingOtpsByEmailAndTypeAndCreatedAtAfter(
                email,
                OtpType.FORGOT_PASSWORD,
                cooldownTime,
                now
        );

        if (recentRequests > 0) {
            throw new IllegalStateException("Please wait 30 seconds before requesting a new OTP");
        }

        // Invalidate any existing pending OTPs
        otpRepository.invalidateAllPendingOtpsByUserIdAndType(user.getId(), OtpType.FORGOT_PASSWORD);

        // Generate new OTP
        String otp = generateOtp();

        // Create and save OTP
        UserOtp userOtp = UserOtp.builder()
                .user(user)
                .email(email)
                .otp(otp)
                .type(OtpType.FORGOT_PASSWORD)
                .expirationSeconds(otpExpirationSeconds)
                .attemptCount(0)
                .build();

        otpRepository.save(userOtp);

        // Send OTP email asynchronously
        notificationClient.sendPasswordResetOtp(user, otp);

        // Audit log
        auditLogService.logUserEvent(
                user.getUsername(),
                "PASSWORD_RESET_OTP_RESENT",
                "UserOtp",
                "New password reset OTP created and sent",
                null,
                true,
                null,
                null
        );

        log.info("New password reset OTP created for user={} email={} expiresAt={}",
                user.getId(), email, userOtp.getExpiresAt());
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        // Validate reset token
        if (!jwtService.isTokenValid(resetToken)) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        // Extract user ID from token
        Long userId = jwtService.extractUserId(resetToken);
        if (userId == null) {
            throw new IllegalArgumentException("Invalid reset token");
        }

        // Verify session
        if (!verifiedOtpSessions.containsKey(resetToken)) {
            throw new IllegalArgumentException("Reset token not found or already used");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Remove verified session
        verifiedOtpSessions.remove(resetToken);

        // Invalidate all OTPs for this user
        otpRepository.invalidateAllPendingOtpsByUserIdAndType(userId, OtpType.FORGOT_PASSWORD);

        // Audit log
        auditLogService.logUserEvent(
                user.getUsername(),
                "PASSWORD_RESET_SUCCESS",
                "User",
                "Password reset successfully",
                null,
                true,
                null,
                null
        );

        log.info("Password reset successfully for user={}", userId);
    }

    @Scheduled(cron = "0 */5 * * * *") // Run every 5 minutes
    @Transactional
    public void cleanupExpiredOtps() {
        int deleted = otpRepository.deleteExpiredOtps(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired OTPs", deleted);
        }
    }

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void cleanupExpiredSessions() {
        // Clean up expired sessions
        verifiedOtpSessions.entrySet().removeIf(entry -> !jwtService.isTokenValid(entry.getKey()));
        log.info("Cleaned up expired OTP sessions. Remaining: {}", verifiedOtpSessions.size());
    }

    private String generateOtp() {
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    private String generateResetToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", "PASSWORD_RESET");
        claims.put("email", user.getEmail());

        return jwtService.generatePasswordResetToken(claims, user, resetTokenExpirationSeconds);
    }

    private void checkRateLimit(String email) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownTime = now.minusSeconds(requestCooldownSeconds);

        // Only count pending (unverified and unexpired) OTPs for rate limiting
        // This prevents blocking users who have already verified their OTP or whose OTP has expired
        int recentRequests = otpRepository.countPendingOtpsByEmailAndTypeAndCreatedAtAfter(
                email,
                OtpType.FORGOT_PASSWORD,
                cooldownTime,
                now
        );

        if (recentRequests > 0) {
            throw new IllegalStateException(
                    "Please wait " + requestCooldownSeconds + " seconds before requesting a new OTP"
            );
        }
    }
}
