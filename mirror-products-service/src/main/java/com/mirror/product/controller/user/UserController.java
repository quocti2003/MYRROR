package com.mirror.product.controller.user;

import com.mirror.product.dto.user.ChangePasswordRequest;
import com.mirror.product.dto.user.UpdateProfileRequest;
import com.mirror.product.dto.user.UserResponse;
import com.mirror.product.dto.user.AdminCreateUserRequest;
import com.mirror.product.dto.user.AdminUpdateUserRequest;
import com.mirror.product.dto.user.UserContactResponse;
import com.mirror.product.service.user.AuthorizationService;
import com.mirror.product.service.user.RateLimitingService;
import com.mirror.product.service.user.UserService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    private final AuthorizationService authorizationService;
    private final RateLimitingService rateLimitingService;
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserProfile(HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded"));
        }
        
        try {
            UserResponse userResponse = userService.getCurrentUserProfile();
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            log.error("Failed to get user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve profile"));
        }
    }
    
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'VENDOR', 'DESIGNER', 'SUPER_ADMIN', 'IT_ADMIN', 'PRODUCTION_OPS', 'SALES_CUSTOMER_OPS', 'CREATIVE_DESIGN', 'MARKETING', 'FINANCE', 'LEGAL')")
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded"));
        }
        
        try {
            UserResponse userResponse = userService.updateProfile(request, httpRequest);
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            log.error("Failed to update profile", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'VENDOR', 'DESIGNER', 'SUPER_ADMIN', 'IT_ADMIN', 'PRODUCTION_OPS', 'SALES_CUSTOMER_OPS', 'CREATIVE_DESIGN', 'MARKETING', 'FINANCE', 'LEGAL')")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded"));
        }
        
        try {
            userService.changePassword(request, httpRequest);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to change password", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessUser(#userId)")
    public ResponseEntity<?> getUserById(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded"));
        }
        
        try {
            UserResponse userResponse = userService.getUserById(userId);
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            log.error("Failed to get user by ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("User not found"));
        }
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            Pageable pageable,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded"));
        }
        
        try {
            Page<UserResponse> users = userService.getAllUsers(pageable);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Failed to get all users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve users"));
        }
    }

    @GetMapping("/{userId}/contact")
    @PreAuthorize("hasAnyRole('ADMIN','SALES') or @authorizationService.canAccessUser(#userId)")
    public ResponseEntity<?> getUserContact(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);

        if (!rateLimitingService.tryConsume(bucket)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded"));
        }

        try {
            UserContactResponse contact = userService.getUserContact(userId);
            return ResponseEntity.ok(contact);
        } catch (Exception e) {
            log.error("Failed to fetch contact for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("User not found"));
        }
    }
    
    @PutMapping("/{userId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> enableUser(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded"));
        }
        
        try {
            userService.enableUser(userId, httpRequest);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User enabled successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to enable user: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{userId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disableUser(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded"));
        }
        
        try {
            userService.disableUser(userId, httpRequest);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User disabled successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to disable user: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') and @authorizationService.canDeleteUser(#userId)")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded"));
        }
        
        try {
            userService.deleteUser(userId, httpRequest);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete user: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUserByAdmin(
            @Valid @RequestBody AdminCreateUserRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded"));
        }
        
        try {
            UserResponse user = userService.createUserByAdmin(request, httpRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (Exception e) {
            log.error("Failed to create user by admin", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        Bucket bucket = rateLimitingService.getGeneralApiBucket(clientIp);
        
        if (!rateLimitingService.tryConsume(bucket)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded"));
        }
        
        try {
            UserResponse user = userService.updateUserByAdmin(userId, request, httpRequest);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Failed to update user with id: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
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
