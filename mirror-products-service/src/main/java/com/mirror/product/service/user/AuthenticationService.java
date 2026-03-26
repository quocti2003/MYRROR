package com.mirror.product.service.user;

import com.mirror.product.dto.user.AuthenticationRequest;
import com.mirror.product.dto.user.AuthenticationResponse;
import com.mirror.product.dto.user.RegisterRequest;
import com.mirror.product.entity.user.Role;
import com.mirror.product.entity.user.User;
import com.mirror.product.exception.user.AccountLockedException;
import com.mirror.product.exception.user.AuthenticationException;
import com.mirror.product.exception.user.UserAlreadyExistsException;
import com.mirror.product.repository.user.RoleRepository;
import com.mirror.product.repository.user.UserRepository;
import com.mirror.product.validation.InputSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final PasswordValidationService passwordValidationService;
    private final InputSanitizer inputSanitizer;
    private final EmailVerificationService emailVerificationService;
    
    @Value("${security.max-failed-attempts:5}")
    private int maxFailedAttempts;
    
    @Value("${security.account-lock-duration:300}")
    private long accountLockDurationSeconds;
    
    @Transactional
    public AuthenticationResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        // Sanitize and validate inputs
        inputSanitizer.validateInputSecurity(request.getUsername(), "username");
        inputSanitizer.validateInputSecurity(request.getEmail(), "email");
        inputSanitizer.validateInputSecurity(request.getFirstName(), "firstName");
        inputSanitizer.validateInputSecurity(request.getLastName(), "lastName");
        inputSanitizer.validateInputSecurity(request.getPhoneNumber(), "phoneNumber");
        if (request.getTitle() != null) {
            inputSanitizer.validateInputSecurity(request.getTitle(), "title");
        }

        request.setUsername(inputSanitizer.sanitizeUsername(request.getUsername()));
        request.setEmail(inputSanitizer.sanitizeEmail(request.getEmail()));
        request.setFirstName(inputSanitizer.sanitizeString(request.getFirstName()));
        request.setLastName(inputSanitizer.sanitizeString(request.getLastName()));
        request.setPhoneNumber(inputSanitizer.sanitizePhoneNumber(request.getPhoneNumber()));
        if (request.getTitle() != null) {
            request.setTitle(inputSanitizer.sanitizeString(request.getTitle()));
        }
        
        log.info("Registration attempt for username: {}", request.getUsername());
        
        if (userRepository.existsByUsername(request.getUsername())) {
            auditLogService.logAuthenticationEvent(
                request.getUsername(), 
                "REGISTRATION_FAILED", 
                "User registration", 
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                false, 
                "Username already exists"
            );
            throw new UserAlreadyExistsException("Username already exists");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            auditLogService.logAuthenticationEvent(
                request.getEmail(), 
                "REGISTRATION_FAILED", 
                "User registration", 
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                false, 
                "Email already exists"
            );
            throw new UserAlreadyExistsException("Email already exists");
        }
        
        passwordValidationService.validatePassword(request.getPassword());
        
        var user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setTitle(request.getTitle());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedBy("SYSTEM");
        user.setUpdatedBy("SYSTEM");
        
        Role defaultRole = roleRepository.findByName("USER")
            .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.setRoles(Set.of(defaultRole));
        
        userRepository.save(user);

        // Send email verification
        emailVerificationService.createAndSendVerificationToken(user);

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        auditLogService.logAuthenticationEvent(
            user.getUsername(),
            "REGISTRATION_SUCCESS",
            "User registration",
            httpRequest.getRemoteAddr(),
            httpRequest.getHeader("User-Agent"),
            true,
            null
        );

        log.info("User registered successfully: {} (email verification sent)", user.getUsername());
        
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpirationTime())
                .tokenType("Bearer")
                .build();
    }
    
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request, HttpServletRequest httpRequest) {
        log.info("Authentication attempt for username: {}", request.getUsername());
        
        User user = userRepository.findByUsername(request.getUsername())
            .orElse(null);
        
        if (user != null) {
            if (isAccountLocked(user)) {
                auditLogService.logAuthenticationEvent(
                    request.getUsername(), 
                    "LOGIN_FAILED", 
                    "User authentication", 
                    httpRequest.getRemoteAddr(),
                    httpRequest.getHeader("User-Agent"),
                    false, 
                    "Account locked"
                );
                throw new AccountLockedException("Account is locked due to multiple failed login attempts");
            }
        }
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );
            
            if (user != null) {
                user.resetFailedLoginAttempts();
                user.updateLastLogin();
                userRepository.save(user);
            }
            
            var jwtToken = jwtService.generateToken((User) authentication.getPrincipal());
            var refreshToken = jwtService.generateRefreshToken((User) authentication.getPrincipal());
            
            auditLogService.logAuthenticationEvent(
                request.getUsername(), 
                "LOGIN_SUCCESS", 
                "User authentication", 
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                true, 
                null
            );
            
            log.info("User authenticated successfully: {}", request.getUsername());
            
            return AuthenticationResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtService.getExpirationTime())
                    .tokenType("Bearer")
                    .build();
                    
        } catch (BadCredentialsException | DisabledException e) {
            if (user != null) {
                user.incrementFailedLoginAttempts();
                userRepository.save(user);
                
                if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                    log.warn("Account locked for user: {} after {} failed attempts", 
                        user.getUsername(), user.getFailedLoginAttempts());
                }
            }
            
            auditLogService.logAuthenticationEvent(
                request.getUsername(), 
                "LOGIN_FAILED", 
                "User authentication", 
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                false, 
                "Invalid credentials"
            );
            
            throw new AuthenticationException("Invalid credentials");
        } catch (org.springframework.security.core.AuthenticationException e) {
            auditLogService.logAuthenticationEvent(
                request.getUsername(), 
                "LOGIN_FAILED", 
                "User authentication", 
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                false, 
                e.getMessage()
            );
            
            throw new AuthenticationException("Authentication failed: " + e.getMessage());
        }
    }
    
    @Transactional
    public AuthenticationResponse refreshToken(String refreshToken, HttpServletRequest httpRequest) {
        if (!jwtService.isTokenValid(refreshToken)) {
            auditLogService.logAuthenticationEvent(
                null, 
                "TOKEN_REFRESH_FAILED", 
                "Token refresh", 
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                false, 
                "Invalid refresh token"
            );
            throw new AuthenticationException("Invalid refresh token");
        }
        
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AuthenticationException("User not found"));
        
        if (isAccountLocked(user) || !user.isEnabled()) {
            auditLogService.logAuthenticationEvent(
                username, 
                "TOKEN_REFRESH_FAILED", 
                "Token refresh", 
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                false, 
                "Account locked or disabled"
            );
            throw new AuthenticationException("Account is locked or disabled");
        }
        
        var newAccessToken = jwtService.generateToken(user);
        var newRefreshToken = jwtService.generateRefreshToken(user);
        
        auditLogService.logAuthenticationEvent(
            username, 
            "TOKEN_REFRESH_SUCCESS", 
            "Token refresh", 
            httpRequest.getRemoteAddr(),
            httpRequest.getHeader("User-Agent"),
            true, 
            null
        );
        
        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtService.getExpirationTime())
                .tokenType("Bearer")
                .build();
    }
    
    private boolean isAccountLocked(User user) {
        if (user.getFailedLoginAttempts() == null || user.getFailedLoginAttempts() < maxFailedAttempts) {
            return false;
        }
        
        LocalDateTime lockTime = user.getUpdatedAt().plusSeconds(accountLockDurationSeconds);
        return LocalDateTime.now().isBefore(lockTime);
    }
    
    @Transactional
    public void unlockAccount(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.resetFailedLoginAttempts();
        user.setAccountNonLocked(true);
        userRepository.save(user);
        
        log.info("Account unlocked for user: {}", username);
    }
}