package com.mirror.product.config;

import com.mirror.product.util.JwtSecretsManagerUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter that validates JWT tokens from the Authorization header and sets up Spring Security context.
 * Extracts userId, username, and roles from the JWT claims.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleAuthorizationFilter extends OncePerRequestFilter {

    private final JwtSecretsManagerUtil jwtSecretsManagerUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Claims claims = parseToken(token);

                String username = claims.getSubject();
                Long userId = extractUserId(claims);
                String roles = extractRoles(claims);

                if (username != null) {
                    List<SimpleGrantedAuthority> authorities = parseRolesToAuthorities(roles);

                    log.debug("JWT validated - User: {}, UserId: {}, Roles: {}", username, userId, roles);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);

                    // Store userId as details for later access
                    authentication.setDetails(userId != null ? userId.toString() : null);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                // Continue without authentication - security config will handle unauthorized access
            }
        }

        filterChain.doFilter(request, response);
    }

    private Claims parseToken(String token) {
        String secret = jwtSecretsManagerUtil.getJwtSecret();
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Long extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    private String extractRoles(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof String) {
            return (String) roles;
        }
        return null;
    }

    private List<SimpleGrantedAuthority> parseRolesToAuthorities(String rolesStr) {
        if (rolesStr == null || rolesStr.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(rolesStr.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
