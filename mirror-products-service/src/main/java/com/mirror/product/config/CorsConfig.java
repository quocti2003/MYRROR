package com.mirror.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // Option A: patterns (works with credentials)
        cfg.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.ngrok-free.app",
                "https://*.ngrok.io",
                "https://*.ngrok.app",
                "https://*.ngrok.dev",
                "https://*.ngrok-free.dev",
                "https://*.vercel.app",           //  - Cho phép tất cả Vercel apps
                "https://*.vercel.now.sh",        //  - Vercel preview URLs
                "https://*.now.sh",
                "https://www.mirrorfuturediamond.com",
                "https://mirrorfuturediamond.com",
                "https://*.mirrorfuturediamond.com"
        ));
        // Option B (instead): use setAllowedOrigins(...) with exact hosts only

        cfg.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(Arrays.asList(
                "Authorization","Content-Type","Accept","Origin","X-Requested-With",
                "Access-Control-Request-Method","Access-Control-Request-Headers",
                "Cache-Control","Pragma","X-User-Id","X-Pod-Attribution"
        ));
        cfg.setExposedHeaders(Arrays.asList("X-Total-Count","X-Total-Pages","Authorization","Content-Type"));
        cfg.setAllowCredentials(true);  // ok with patterns; not ok with "*"
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg); // apply to all endpoints
        return source;
    }
}