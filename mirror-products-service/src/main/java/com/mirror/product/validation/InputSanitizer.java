package com.mirror.product.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@Slf4j
public class InputSanitizer {
    
    private static final Pattern HTML_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?i)<script[^>]*>.*?</script>");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror|onclick).*"
    );
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i).*(javascript:|vbscript:|onload|onerror|onclick|onfocus|onblur|onchange|onsubmit).*"
    );
    
    public String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        
        String sanitized = input.trim();
        
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        sanitized = HTML_PATTERN.matcher(sanitized).replaceAll("");
        
        sanitized = sanitized.replaceAll("[<>\"'&]", "");
        
        if (sanitized.length() > 1000) {
            log.warn("Input string truncated from {} to 1000 characters", sanitized.length());
            sanitized = sanitized.substring(0, 1000);
        }
        
        return sanitized;
    }
    
    public String sanitizeUsername(String username) {
        if (username == null) {
            return null;
        }
        
        String sanitized = username.trim().toLowerCase();
        
        sanitized = sanitized.replaceAll("[^a-z0-9._-]", "");
        
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized;
    }
    
    public String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }
        
        String sanitized = email.trim().toLowerCase();
        
        sanitized = sanitized.replaceAll("[^a-z0-9@._-]", "");
        
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        
        return sanitized;
    }
    
    public String sanitizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        
        String sanitized = phoneNumber.trim();
        
        sanitized = sanitized.replaceAll("[^0-9+()\\s-]", "");
        
        if (sanitized.length() > 20) {
            sanitized = sanitized.substring(0, 20);
        }
        
        return sanitized;
    }
    
    public boolean containsSqlInjection(String input) {
        if (input == null) {
            return false;
        }
        
        return SQL_INJECTION_PATTERN.matcher(input).matches();
    }
    
    public boolean containsXss(String input) {
        if (input == null) {
            return false;
        }
        
        return XSS_PATTERN.matcher(input).matches();
    }
    
    public void validateInputSecurity(String input, String fieldName) {
        if (input == null) {
            return;
        }
        
        if (containsSqlInjection(input)) {
            log.error("SQL injection attempt detected in field: {}", fieldName);
            throw new SecurityException("Invalid input detected in " + fieldName);
        }
        
        if (containsXss(input)) {
            log.error("XSS attempt detected in field: {}", fieldName);
            throw new SecurityException("Invalid input detected in " + fieldName);
        }
    }
}