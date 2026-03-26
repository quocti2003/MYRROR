package com.mirror.product.controller;

import com.mirror.product.config.R2Config;
import com.mirror.product.dto.FileUploadResponse;
import com.mirror.product.service.R2Service;
import com.mirror.product.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cloudflare R2 Controller
 *
 * Provides endpoints for file upload/download operations with R2.
 * Only activated when cloudflare.r2.enabled=true
 * Includes rate limiting to prevent spam uploads.
 */
@RestController
@RequestMapping("/api/r2")
@ConditionalOnProperty(name = "cloudflare.r2.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class R2Controller {

    private final R2Service r2Service;
    private final R2Config r2Config;
    private final RateLimitService rateLimitService;

    // Staff roles - no rate limit
    private static final Set<String> STAFF_ROLES = Set.of(
        "ROLE_SUPER_ADMIN",
        "ROLE_ADMIN",
        "ROLE_IT_ADMIN",
        "ROLE_PRODUCTION_OPS",
        "ROLE_SALES_CUSTOMER_OPS",
        "ROLE_FINANCE",
        "ROLE_MARKETING",
        "ROLE_CREATIVE_DESIGN",
        "ROLE_LEGAL",
        "ROLE_VENDOR",
        "ROLE_DESIGNER"
    );

    /**
     * Upload file directly to R2
     * POST /api/r2/upload
     *
     * Protected by:
     * - Authentication required (handled by SecurityConfig)
     * - Staff roles: no rate limit
     * - USER role: 20 uploads per hour
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder,
            HttpServletRequest request) {
        try {
            // Check if user has staff role (uses SecurityContextHolder)
            boolean isStaff = isStaffUser();
            String clientIp = getClientIp(request);

            // Rate limit check - only for non-staff users (USER role)
            if (!isStaff) {
                if (!rateLimitService.isAllowed(clientIp)) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Rate limit exceeded");
                    error.put("message", "Too many uploads. Please try again later.");
                    error.put("retryAfterSeconds", rateLimitService.getSecondsUntilReset(clientIp));
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
                }
            }

            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            long size = file.getSize();

            FileUploadResponse response = r2Service.uploadFile(
                    folder,
                    originalFilename,
                    file.getInputStream(),
                    size,
                    contentType
            );

            // Add rate limit info to response headers (for non-staff)
            if (!isStaff) {
                return ResponseEntity.ok()
                        .header("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingRequests(clientIp)))
                        .body(response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if user has staff role (no rate limit)
     * Uses SecurityContextHolder same as other controllers in the system
     */
    private boolean isStaffUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(STAFF_ROLES::contains);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Get first IP if multiple (client IP)
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Get presigned URL for direct frontend upload
     * POST /api/r2/presigned-upload
     *
     * Frontend can use this URL to upload directly to R2
     */
    @PostMapping("/presigned-upload")
    public ResponseEntity<Map<String, String>> getPresignedUploadUrl(
            @RequestParam("filename") String filename,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder,
            @RequestParam(value = "contentType", defaultValue = "application/octet-stream") String contentType) {
        try {
            // Generate unique key
            String safeFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            String key = normalizeFolder(folder) + UUID.randomUUID() + "_" + safeFilename;

            // Generate presigned URL (valid for 15 minutes)
            String uploadUrl = r2Service.generateUploadUrl(key, contentType, Duration.ofMinutes(15));

            // Get public URL that will be accessible after upload
            String publicUrl = r2Config.getPublicUrl(key);

            Map<String, String> response = new HashMap<>();
            response.put("uploadUrl", uploadUrl);
            response.put("key", key);
            response.put("publicUrl", publicUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating presigned URL: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get public URL for a file
     * GET /api/r2/public-url
     */
    @GetMapping("/public-url")
    public ResponseEntity<Map<String, String>> getPublicUrl(@RequestParam("key") String key) {
        try {
            String publicUrl = r2Config.getPublicUrl(key);

            Map<String, String> response = new HashMap<>();
            response.put("publicUrl", publicUrl);
            response.put("key", key);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting public URL: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete file from R2
     * DELETE /api/r2/files/{key}
     */
    @DeleteMapping("/files")
    public ResponseEntity<Map<String, String>> deleteFile(@RequestParam("key") String key) {
        try {
            r2Service.deleteFile(key);

            Map<String, String> response = new HashMap<>();
            response.put("message", "File deleted successfully");
            response.put("key", key);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if R2 is properly configured
     * GET /api/r2/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("provider", "Cloudflare R2");
        response.put("bucket", r2Config.getBucketName());
        response.put("publicUrlBase", r2Config.getPublicUrlBase());
        return ResponseEntity.ok(response);
    }

    private String normalizeFolder(String folder) {
        if (folder == null || folder.trim().isEmpty()) {
            return "";
        }
        String normalized = folder.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        return normalized;
    }
}
