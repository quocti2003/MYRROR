package com.mirror.product.controller.user;

import com.mirror.product.dto.user.*;
import com.mirror.product.repository.user.UserRepository;
import com.mirror.product.service.pod.PodUserAttributionService;
import com.mirror.product.service.user.AuthenticationService;
import com.mirror.product.service.user.PasswordResetService;
import com.mirror.product.service.user.RateLimitingService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RateLimitingService rateLimitingService;
    private final com.mirror.product.service.user.EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final PodUserAttributionService podUserAttributionService;
    private final UserRepository userRepository;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getRegistrationBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            log.warn("Registration rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded. Please try again later."));
        }
        
        try {
            AuthenticationResponse response = authenticationService.register(request, httpRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Registration failed for username: {}", request.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Authentication attempt for username: {}", request.getUsername());
        
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getAuthenticationBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            log.warn("Authentication rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded. Please try again later."));
        }
        
        try {
            AuthenticationResponse response = authenticationService.authenticate(request, httpRequest);

            // Link user-POD attribution from cookie (non-blocking, log errors only)
            try {
                userRepository.findByUsername(request.getUsername())
                        .ifPresent(user -> podUserAttributionService.linkUserFromCookie(user.getId(), httpRequest));
            } catch (Exception attrEx) {
                log.warn("Failed to process POD attribution on login: {}", attrEx.getMessage());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Authentication failed for username: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            log.warn("Token refresh rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded. Please try again later."));
        }
        
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Refresh token is required"));
        }
        
        try {
            AuthenticationResponse response = authenticationService.refreshToken(refreshToken, httpRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Backend is running via ngrok!");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            emailVerificationService.verifyEmail(token);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email verified successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Email verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Email verification error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to verify email"));
        }
    }

    @PostMapping("/resend-verification-email")
    public ResponseEntity<?> resendVerificationEmail(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);

        if (!rateLimitingService.tryConsume(bucket)) {
            log.warn("Resend verification email rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded. Please try again later."));
        }

        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Email is required"));
        }

        try {
            emailVerificationService.resendVerificationEmail(email);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verification email sent");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Resend verification email failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("Resend verification email failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Resend verification email error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to send verification email"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);

        if (!rateLimitingService.tryConsume(bucket)) {
            log.warn("Forgot password rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded. Please try again later."));
        }

        try {
            passwordResetService.requestPasswordReset(request.getEmail());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "If the email exists, a password reset OTP has been sent.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Forgot password failed: {}", e.getMessage());
            // Don't reveal if email exists or not for security
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "If the email exists, a password reset OTP has been sent.");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.error("Forgot password rate limited: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Forgot password error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to process password reset request"));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);

        if (!rateLimitingService.tryConsume(bucket)) {
            log.warn("Verify OTP rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded. Please try again later."));
        }

        try {
            VerifyOtpResponse response = passwordResetService.verifyOtp(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("OTP verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("OTP verification error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to verify OTP"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);

        if (!rateLimitingService.tryConsume(bucket)) {
            log.warn("Reset password rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded. Please try again later."));
        }

        try {
            passwordResetService.resetPassword(request.getResetToken(), request.getNewPassword());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Password reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Password reset error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to reset password"));
        }
    }

    @PostMapping("/resend-password-reset-otp")
    public ResponseEntity<?> resendPasswordResetOtp(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);

        if (!rateLimitingService.tryConsume(bucket)) {
            log.warn("Resend password reset OTP rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded. Please try again later."));
        }

        try {
            passwordResetService.resendPasswordResetOtp(request.getEmail());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset OTP has been resent to your email.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Resend password reset OTP failed: {}", e.getMessage());
            // Don't reveal if email exists or not for security
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "If the email exists, a password reset OTP has been resent.");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.error("Resend password reset OTP rate limited: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Resend password reset OTP error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to resend password reset OTP"));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }
}