package com.mirror.product.service.user;

import com.mirror.product.dto.user.ChangePasswordRequest;
import com.mirror.product.dto.user.UpdateProfileRequest;
import com.mirror.product.dto.user.UserContactResponse;
import com.mirror.product.dto.user.UserResponse;
import com.mirror.product.dto.user.AdminCreateUserRequest;
import com.mirror.product.dto.user.AdminUpdateUserRequest;
import com.mirror.product.entity.user.Permission;
import com.mirror.product.entity.user.Role;
import com.mirror.product.entity.user.User;
import com.mirror.product.repository.user.UserRepository;
import com.mirror.product.repository.user.RoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthorizationService authorizationService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;
    private final AuditLogService auditLogService;
    
    public UserResponse getCurrentUserProfile() {
        User currentUser = authorizationService.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        log.debug("Current user profile - Username: {}, Title: {}, Email: {}", 
                  currentUser.getUsername(), currentUser.getTitle(), currentUser.getEmail());
        
        return mapToUserResponse(currentUser);
    }
    
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request, HttpServletRequest httpRequest) {
        User currentUser = authorizationService.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }
        log.debug("[BACKEND] User data BEFORE update: FirstName={}, LastName={}, Email={}, Title={}",
                currentUser.getFirstName(),currentUser.getLastName(), currentUser.getEmail(), currentUser.getTitle());
        // BỔ SUNG LOGIC CẬP NHẬT CHO TITLE VÀ DATE OF BIRTH
        if (request.getTitle() != null) {
            currentUser.setTitle(request.getTitle());
        }

        if (request.getDateOfBirth() != null) {
            currentUser.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getNationality() != null) {
            currentUser.setNationality(request.getNationality());
        }
        
        if (request.getEmail() != null && !request.getEmail().equals(currentUser.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            currentUser.setEmail(request.getEmail());
        }
        
        if (request.getFirstName() != null) {
            currentUser.setFirstName(request.getFirstName());
        }
        
        if (request.getLastName() != null) {
            currentUser.setLastName(request.getLastName());
        }
        
        if (request.getPhoneNumber() != null) {
            currentUser.setPhoneNumber(request.getPhoneNumber());
        }
        
        currentUser.setUpdatedBy(currentUser.getUsername());
        User savedUser = userRepository.save(currentUser);
        
        auditLogService.logUserEvent(
            currentUser.getUsername(),
            "PROFILE_UPDATED",
            "User profile",
            httpRequest.getRemoteAddr(),
            httpRequest.getHeader("User-Agent"),
            true,
            "Profile updated successfully",
            null
        );
        
        log.info("Profile updated for user: {}", currentUser.getUsername());
        return mapToUserResponse(savedUser);
    }
    
    @Transactional
    public void changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest) {
        User currentUser = authorizationService.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            auditLogService.logUserEvent(
                currentUser.getUsername(),
                "PASSWORD_CHANGE_FAILED",
                "Password change",
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                false,
                null,
                "Current password is incorrect"
            );
            throw new RuntimeException("Current password is incorrect");
        }
        
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }
        
        passwordValidationService.validatePassword(request.getNewPassword());
        
        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        currentUser.updatePasswordChangedAt();
        currentUser.setUpdatedBy(currentUser.getUsername());
        userRepository.save(currentUser);
        
        auditLogService.logUserEvent(
            currentUser.getUsername(),
            "PASSWORD_CHANGED",
            "Password change",
            httpRequest.getRemoteAddr(),
            httpRequest.getHeader("User-Agent"),
            true,
            "Password changed successfully",
            null
        );
        
        log.info("Password changed for user: {}", currentUser.getUsername());
    }
    
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return mapToUserResponse(user);
    }

    public UserContactResponse getUserContact(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserContactResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
    
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::mapToUserResponse);
    }
    
    @Transactional
    public void enableUser(Long userId, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setEnabled(true);
        user.setUpdatedBy(authorizationService.getCurrentUsername());
        userRepository.save(user);
        
        auditLogService.logUserEvent(
            authorizationService.getCurrentUsername(),
            "USER_ENABLED",
            "User management",
            httpRequest.getRemoteAddr(),
            httpRequest.getHeader("User-Agent"),
            true,
            "User " + user.getUsername() + " enabled",
            null
        );
        
        log.info("User enabled: {} by {}", user.getUsername(), authorizationService.getCurrentUsername());
    }
    
    @Transactional
    public void disableUser(Long userId, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        User currentUser = authorizationService.getCurrentUser();
        if (currentUser != null && currentUser.getId().equals(userId)) {
            throw new RuntimeException("Cannot disable your own account");
        }
        
        user.setEnabled(false);
        user.setUpdatedBy(authorizationService.getCurrentUsername());
        userRepository.save(user);
        
        auditLogService.logUserEvent(
            authorizationService.getCurrentUsername(),
            "USER_DISABLED",
            "User management",
            httpRequest.getRemoteAddr(),
            httpRequest.getHeader("User-Agent"),
            true,
            "User " + user.getUsername() + " disabled",
            null
        );
        
        log.info("User disabled: {} by {}", user.getUsername(), authorizationService.getCurrentUsername());
    }
    
    @Transactional
    public void deleteUser(Long userId, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        User currentUser = authorizationService.getCurrentUser();
        if (currentUser != null && currentUser.getId().equals(userId)) {
            throw new RuntimeException("Cannot delete your own account");
        }
        
        String deletedUsername = user.getUsername();
        userRepository.delete(user);
        
        auditLogService.logUserEvent(
            authorizationService.getCurrentUsername(),
            "USER_DELETED",
            "User management",
            httpRequest.getRemoteAddr(),
            httpRequest.getHeader("User-Agent"),
            true,
            "User " + deletedUsername + " deleted",
            null
        );
        
        log.info("User deleted: {} by {}", deletedUsername, authorizationService.getCurrentUsername());
    }
    
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .title(user.getTitle())
                .dateOfBirth(user.getDateOfBirth())
                .nationality(user.getNationality())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .enabled(user.getEnabled())
                .accountNonExpired(user.getAccountNonExpired())
                .accountNonLocked(user.getAccountNonLocked())
                .credentialsNonExpired(user.getCredentialsNonExpired())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .permissions(user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
    
    @Transactional
    public UserResponse createUserByAdmin(AdminCreateUserRequest request, HttpServletRequest httpRequest) {
        User currentUser = authorizationService.getCurrentUser();
        
        // Validate that current user is admin
        if (currentUser == null || !authorizationService.hasRole("ADMIN")) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Validate password
        passwordValidationService.validatePassword(request.getPassword());
        
        // Create new user
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdBy(currentUser.getUsername())
                .updatedBy(currentUser.getUsername())
                .build();

        // Add role to user
        if (request.getRole() != null) {
            log.info("Role assignment requested: {} for user: {}", request.getRole(), request.getUsername());
            Role role = roleRepository.findByName(request.getRole())
                    .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRole()));
            newUser.setRoles(Set.of(role));
        } else {
            // Default to USER role if no role specified
            Role defaultRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            newUser.setRoles(Set.of(defaultRole));
        }

        User savedUser = userRepository.save(newUser);
        
        // Log the admin action
        auditLogService.logUserEvent(
            currentUser.getUsername(),
            "ADMIN_CREATE_USER",
            "Created user: " + savedUser.getUsername(),
            httpRequest.getRemoteAddr(),
            httpRequest.getHeader("User-Agent"),
            true,
            "User created successfully by admin",
            null
        );
        
        return mapToUserResponse(savedUser);
    }
    
    @Transactional
    public UserResponse updateUserByAdmin(Long userId, AdminUpdateUserRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Check if email is being changed and if it already exists for another user
        if (!request.getEmail().equals(user.getEmail())) {
            userRepository.findByEmail(request.getEmail())
                .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                .ifPresent(existingUser -> {
                    throw new RuntimeException("Email already exists");
                });
        }
        
        // Check if username is being changed and if it already exists for another user
        if (!request.getUsername().equals(user.getUsername())) {
            userRepository.findByUsername(request.getUsername())
                .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                .ifPresent(existingUser -> {
                    throw new RuntimeException("Username already exists");
                });
        }
        
        // Update basic fields
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEnabled(request.getEnabled());
        
        // Update password only if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            passwordValidationService.validatePassword(request.getPassword());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        // Update role if changed
        if (!getCurrentPrimaryRole(user).equals(request.getRole())) {
            Role newRole = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RuntimeException("Invalid role: " + request.getRole()));
            user.setRoles(java.util.Set.of(newRole));
        }
        
        user.setUpdatedBy("ADMIN");
        User savedUser = userRepository.save(user);
        
        // Log the update
        auditLogService.logUserEvent(
            String.valueOf(savedUser.getId()),
            "UPDATE",
            "User updated by admin",
            httpRequest.getRemoteAddr(),
            httpRequest.getHeader("User-Agent"),
            true,
            "User updated successfully by admin",
            null
        );
        
        log.info("User updated successfully by admin: {}", savedUser.getUsername());
        return mapToUserResponse(savedUser);
    }
    
    private String getCurrentPrimaryRole(User user) {
        return user.getRoles().stream()
            .map(Role::getName)
            .filter(roleName -> roleName.equals("USER") || roleName.equals("ADMIN") || roleName.equals("SUPER_ADMIN") || roleName.equals("DESIGNER"))
            .findFirst()
            .orElse("USER");
    }
}
