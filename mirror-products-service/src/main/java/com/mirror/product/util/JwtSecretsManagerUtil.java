package com.mirror.product.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@Slf4j
@Component
public class JwtSecretsManagerUtil {

    @Value("${jwt.secret.name:mirror-user-service/jwt-secret}")
    private String secretName;
    
    @Value("${jwt.secret.region:ap-southeast-1}")
    private String regionStr;
    
    @Value("${jwt.secret.use-secrets-manager:false}")
    private boolean useSecretsManager;
    
    @Value("${jwt.secret:}")
    private String fallbackSecret;
    
    private String cachedSecret;
    
    public String getJwtSecret() {
        if (!useSecretsManager) {
            log.debug("Secrets Manager disabled, using fallback JWT secret");
            if (fallbackSecret != null && !fallbackSecret.trim().isEmpty()) {
                return fallbackSecret;
            } else {
                log.error("❌ JWT Secrets Manager disabled but no fallback secret configured. Please set JWT_SECRET environment variable.");
                throw new RuntimeException("JWT secret not configured - set JWT_SECRET environment variable");
            }
        }
        
        if (cachedSecret != null) {
            return cachedSecret;
        }
        
        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(regionStr))
                .build()) {
            
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            
            GetSecretValueResponse response = client.getSecretValue(request);
            cachedSecret = response.secretString();
            
            log.info("✅ JWT secret retrieved from AWS Secrets Manager");
            return cachedSecret;
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to retrieve JWT secret from Secrets Manager: {}", e.getMessage());
            if (fallbackSecret != null && !fallbackSecret.trim().isEmpty()) {
                log.info("Using fallback JWT secret");
                return fallbackSecret;
            } else {
                log.error("❌ No fallback JWT secret available. Please set JWT_SECRET environment variable or fix Secrets Manager access.");
                throw new RuntimeException("JWT secret not available from Secrets Manager and no fallback configured");
            }
        }
    }
    
    public void clearCache() {
        cachedSecret = null;
        log.info("JWT secret cache cleared");
    }
}