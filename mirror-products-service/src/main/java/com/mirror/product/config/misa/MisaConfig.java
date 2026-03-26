package com.mirror.product.config.misa;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableAsync
public class MisaConfig {

    @Bean
    @ConfigurationProperties(prefix = "misa.api")
    public MisaApiProperties misaApiProperties() {
        return new MisaApiProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "misa.sync")
    public MisaSyncProperties misaSyncProperties() {
        return new MisaSyncProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "misa.amis")
    public MisaAmisApiProperties misaAmisApiProperties() {
        return new MisaAmisApiProperties();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)); // 1MB
    }

    public static class MisaApiProperties {
        private String domain;
        private String appId;
        private String secretKey;
        private String baseUrl;

        // Getters and setters
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class MisaSyncProperties {
        private boolean enabled = true;
        private long autoSyncInterval = 3600000; // 1 hour
        private int batchSize = 100;
        private int retryAttempts = 3;
        private long retryDelay = 5000; // 5 seconds

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public long getAutoSyncInterval() { return autoSyncInterval; }
        public void setAutoSyncInterval(long autoSyncInterval) { this.autoSyncInterval = autoSyncInterval; }

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

        public int getRetryAttempts() { return retryAttempts; }
        public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }

        public long getRetryDelay() { return retryDelay; }
        public void setRetryDelay(long retryDelay) { this.retryDelay = retryDelay; }
    }

    /**
     * Configuration properties for MISA AMIS ACT Open API
     * This is separate from the existing eShop API (MisaApiProperties)
     */
    public static class MisaAmisApiProperties {
        private String appId;
        private String accessCode;
        private String baseUrl;
        private String orgCompanyCode;
        private String callbackUrl;
        private String branchId;

        // Getters and setters
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }

        public String getAccessCode() { return accessCode; }
        public void setAccessCode(String accessCode) { this.accessCode = accessCode; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getOrgCompanyCode() { return orgCompanyCode; }
        public void setOrgCompanyCode(String orgCompanyCode) { this.orgCompanyCode = orgCompanyCode; }

        public String getCallbackUrl() { return callbackUrl; }
        public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

        public String getBranchId() { return branchId; }
        public void setBranchId(String branchId) { this.branchId = branchId; }
    }
}
