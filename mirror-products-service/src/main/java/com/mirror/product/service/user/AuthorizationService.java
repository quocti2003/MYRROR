package com.mirror.product.service.user;

import com.mirror.product.entity.user.Permission;
import com.mirror.product.entity.user.Role;
import com.mirror.product.entity.user.User;
import com.mirror.product.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final UserRepository userRepository;
    
    public boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(permission));
    }
    
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
    
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasAllRoles(String... roles) {
        for (String role : roles) {
            if (!hasRole(role)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean hasAnyPermission(String... permissions) {
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasAllPermissions(String... permissions) {
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean canAccessUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        if (hasRole("ADMIN") || hasRole("SUPER_ADMIN")) {
            return true;
        }
        
        if (authentication.getPrincipal() instanceof User currentUser) {
            return currentUser.getId().equals(userId);
        }
        
        return false;
    }
    
    public boolean canModifyUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        if (hasRole("ADMIN") || hasRole("SUPER_ADMIN")) {
            return true;
        }
        
        if (hasPermission("USER_MODIFY") && authentication.getPrincipal() instanceof User currentUser) {
            return currentUser.getId().equals(userId);
        }
        
        return false;
    }
    
    public boolean canDeleteUser(Long userId) {
        if (!hasRole("ADMIN") && !hasRole("SUPER_ADMIN")) {
            return false;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof User currentUser) {
            return !currentUser.getId().equals(userId);
        }
        
        return true;
    }
    
    public boolean canManageRoles() {
        return hasRole("SUPER_ADMIN") || hasPermission("ROLE_MANAGE");
    }
    
    public boolean canManagePermissions() {
        return hasRole("SUPER_ADMIN") || hasPermission("PERMISSION_MANAGE");
    }
    
    public Set<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }
        
        if (authentication.getPrincipal() instanceof User user) {
            return user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());
        }
        
        return Set.of();
    }
    
    public Set<String> getCurrentUserPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }
        
        if (authentication.getPrincipal() instanceof User user) {
            return user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(Permission::getName)
                    .collect(Collectors.toSet());
        }
        
        return Set.of();
    }
    
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        return authentication.getName();
    }
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("No authentication or not authenticated");
            return null;
        }

        Object principal = authentication.getPrincipal();
        log.debug("Principal type: {}", principal != null ? principal.getClass().getName() : "null");

        // Direct User instance (ideal case)
        if (principal instanceof User user) {
            return user;
        }

        // UserDetails but not User - load from database
        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            log.debug("Principal is UserDetails with username: {}, loading user from database", username);
            return userRepository.findByUsernameWithRolesAndPermissions(username)
                    .orElse(null);
        }

        // Fallback: Try to get username from authentication name
        String username = authentication.getName();
        if (username != null && !username.equals("anonymousUser")) {
            log.debug("Loading user by authentication name: {}", username);
            return userRepository.findByUsernameWithRolesAndPermissions(username)
                    .orElse(null);
        }

        return null;
    }
}