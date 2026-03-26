package com.mirror.product.config;

import com.mirror.product.security.JwtAuthenticationEntryPoint;
import com.mirror.product.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final RoleAuthorizationFilter roleAuthorizationFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;

    // Role constants for clarity
    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ADMIN = "ADMIN";
    private static final String IT_ADMIN = "IT_ADMIN";
    private static final String PRODUCTION_OPS = "PRODUCTION_OPS";
    private static final String SALES_CUSTOMER_OPS = "SALES_CUSTOMER_OPS";
    private static final String FINANCE = "FINANCE";
    private static final String MARKETING = "MARKETING";
    private static final String CREATIVE_DESIGN = "CREATIVE_DESIGN";
    private static final String LEGAL = "LEGAL";
    private static final String VENDOR = "VENDOR";
    private static final String DESIGNER = "DESIGNER";
    private static final String CUSTOMER = "CUSTOMER";
    private static final String PARTNER = "PARTNER";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(roleAuthorizationFilter, JwtAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        // Allow OPTIONS for CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Health & actuator endpoints
                        .requestMatchers("/actuator/**", "/health/**").permitAll()

                        // ==================== POD QR CODE SCANNING ====================
                        // Public QR code scan endpoints (no auth required)
                        .requestMatchers("/q/**").permitAll()

                        // ==================== AUTHENTICATION (User Service) ====================
                        // Public auth endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // User profile endpoints (authenticated users)
                        .requestMatchers("/api/v1/users/me").authenticated()
                        .requestMatchers("/api/v1/users/me/**").authenticated()
                        .requestMatchers("/api/v1/users/profile/**").authenticated()
                        // User management (admin only)
                        .requestMatchers("/api/v1/users/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        // Admin user management
                        .requestMatchers("/api/v1/admin/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        // Role matrix management
                        .requestMatchers("/api/v1/role-matrix/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // ==================== WAREHOUSE MANAGEMENT ====================
                        // Warehouse management - production/admin only
                        .requestMatchers(HttpMethod.GET, "/api/warehouses/**").hasAnyRole(PRODUCTION_OPS, SALES_CUSTOMER_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/warehouses/**").hasAnyRole(PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/warehouses/**").hasAnyRole(PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/warehouses/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/warehouses/**").hasAnyRole(PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // ==================== PUBLIC READ ACCESS ====================
                        // Products - public can browse, only staff can modify
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole(CREATIVE_DESIGN, PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole(CREATIVE_DESIGN, PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasAnyRole(CREATIVE_DESIGN, PRODUCTION_OPS, MARKETING, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // Collections - public can browse
                        .requestMatchers(HttpMethod.GET, "/api/collections/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/collections/**").hasAnyRole(PRODUCTION_OPS, MARKETING, CREATIVE_DESIGN, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/collections/**").hasAnyRole(PRODUCTION_OPS, MARKETING, CREATIVE_DESIGN, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/collections/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // Categories - public can browse
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasAnyRole(MARKETING, CREATIVE_DESIGN, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasAnyRole(MARKETING, CREATIVE_DESIGN, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // Locations - public can view store locations
                        .requestMatchers(HttpMethod.GET, "/api/locations/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/locations/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/locations/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/locations/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // ==================== APPOINTMENTS & BLOCKED SLOTS ====================
                        // Public can view slots, unavailable dates, and create appointments (booking)
                        .requestMatchers(HttpMethod.GET, "/api/appointments/slots").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/appointments/unavailable-dates").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/appointments").permitAll()
                        // Blocked slots - specific matchers must come first (before generic /api/appointments/**)
                        .requestMatchers(HttpMethod.GET, "/api/appointments/blocked-slots/**").hasAnyRole(SALES_CUSTOMER_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/appointments/blocked-slots/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/appointments/blocked-slots/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/appointments/blocked-slots/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        // Staff manages appointments (generic matchers last)
                        .requestMatchers(HttpMethod.GET, "/api/appointments/**").hasAnyRole(SALES_CUSTOMER_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/appointments/**").hasAnyRole(SALES_CUSTOMER_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/appointments/*/confirm").hasAnyRole(SALES_CUSTOMER_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/appointments/*/complete").hasAnyRole(SALES_CUSTOMER_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/appointments/*/cancel").hasAnyRole(SALES_CUSTOMER_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/appointments/*/no-show").hasAnyRole(SALES_CUSTOMER_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/appointments/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // ==================== ORDERS ====================
                        // Orders management
                        .requestMatchers("/api/orders/**").hasAnyRole(SALES_CUSTOMER_OPS, FINANCE, PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // ==================== VENDORS ====================
                        // Vendor management (internal staff)
                        .requestMatchers(HttpMethod.GET, "/api/vendors/**").hasAnyRole(PRODUCTION_OPS, SALES_CUSTOMER_OPS, FINANCE, ADMIN, IT_ADMIN, SUPER_ADMIN, VENDOR)
                        .requestMatchers(HttpMethod.POST, "/api/vendors/**").hasAnyRole(PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/vendors/**").hasAnyRole(PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN, VENDOR)
                        .requestMatchers(HttpMethod.DELETE, "/api/vendors/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // ==================== SKU MANAGEMENT ====================
                        // Read operations - staff can view
                        .requestMatchers(HttpMethod.GET, "/api/skus/**").hasAnyRole(CREATIVE_DESIGN, PRODUCTION_OPS, SALES_CUSTOMER_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        // Write operations (create, generate, import) - design/production/admin
                        .requestMatchers(HttpMethod.POST, "/api/skus/**").hasAnyRole(CREATIVE_DESIGN, PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/skus/**").hasAnyRole(CREATIVE_DESIGN, PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/skus/**").hasAnyRole(CREATIVE_DESIGN, PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/skus/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // ==================== COMPONENTS ====================
                        .requestMatchers(HttpMethod.GET, "/api/components/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/components/**").hasAnyRole(CREATIVE_DESIGN, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/components/**").hasAnyRole(CREATIVE_DESIGN, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/components/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // ==================== CERTIFICATES ====================
                        .requestMatchers(HttpMethod.GET, "/api/certificates/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/certificates/**").hasAnyRole(PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/certificates/**").hasAnyRole(PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/certificates/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // ==================== IGI CERTIFICATE LOOKUP ====================
                        // Public read access for IGI report lookups
                        .requestMatchers(HttpMethod.GET, "/api/igi/**").permitAll()
                        // Cache management requires admin access
                        .requestMatchers(HttpMethod.POST, "/api/igi/**").hasAnyRole(PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/igi/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // ==================== STOCK RECONCILIATION ====================
                        // Stock reconciliation records - inventory management
                        .requestMatchers(HttpMethod.GET, "/api/stock-reconciliation/**").hasAnyRole(PRODUCTION_OPS, FINANCE, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/stock-reconciliation/**").hasAnyRole(PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/stock-reconciliation/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // ==================== PRODUCT OPS ====================
                        // Product operations dashboard endpoints
                        .requestMatchers("/api/product-ops/**").hasAnyRole(CREATIVE_DESIGN, PRODUCTION_OPS, SALES_CUSTOMER_OPS, MARKETING, ADMIN, IT_ADMIN, SUPER_ADMIN)

                        // ==================== R2 STORAGE ====================
                        // R2 health check - public
                        .requestMatchers(HttpMethod.GET, "/api/r2/health").permitAll()
                        // R2 upload - requires authentication (rate limiting in controller)
                        // Staff roles: no rate limit
                        // USER role: 20 uploads per hour
                        .requestMatchers("/api/r2/**").authenticated()

                        // ==================== POD MANAGEMENT ====================
                        // POD Admin endpoints - requires POD management permissions
                        .requestMatchers("/api/v1/admin/pod-partners/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers("/api/v1/admin/pods/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers("/api/v1/admin/pod-qrcodes/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers("/api/v1/admin/pod-attributions/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        .requestMatchers("/api/v1/admin/pod-commissions/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        // Phygital POD - Admin wholesale management
                        .requestMatchers("/api/v1/admin/wholesale/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN)
                        // POD Partner portal - requires PARTNER role (includes /phygital sub-path)
                        .requestMatchers("/api/v1/partner/**").hasRole(PARTNER)

                        // ==================== PRINTING/LABEL MODULE ====================
                        // Printing APIs - permit all (internal tool)
                        .requestMatchers("/api/printing/**").permitAll()

                        // ==================== INTERNAL ENDPOINTS ====================
                        // Internal API for service-to-service communication
                        .requestMatchers("/internal/**").permitAll()

                        // ==================== RFID SCANNER (for PDA devices) ====================
                        // Public endpoints for PDA scanning - no auth required
                        .requestMatchers(HttpMethod.POST, "/api/v1/rfid/scan").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rfid/scan-barcode/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rfid/check-scan-data").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/rfid/add-test-product").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/rfid/update-product-price").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rfid/tags/*/product").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rfid/tags/*").permitAll()
                        // Admin RFID management requires auth
                        .requestMatchers("/api/v1/rfid/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN, PRODUCTION_OPS)

                        // ==================== LABEL PRINTING ====================
                        // Label/Print endpoints require admin access
                        .requestMatchers("/api/v1/admin/labels/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN, PRODUCTION_OPS)
                        .requestMatchers("/api/v1/admin/label-templates/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN, PRODUCTION_OPS)
                        .requestMatchers("/api/v1/admin/print-jobs/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN, PRODUCTION_OPS)
                        .requestMatchers("/api/v1/admin/printer/**").hasAnyRole(ADMIN, IT_ADMIN, SUPER_ADMIN, PRODUCTION_OPS)

                        // ==================== DEFAULT ====================
                        // Any other authenticated request requires at least USER role
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}