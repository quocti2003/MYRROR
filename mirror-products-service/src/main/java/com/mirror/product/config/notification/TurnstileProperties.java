package com.mirror.product.config.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.security.turnstile")
public class TurnstileProperties {

    /**
     * Set to true to force Turnstile validation. If false, validation is skipped even when a secret is present.
     */
    private boolean enabled = false;

    /**
     * Cloudflare Turnstile secret key.
     */
    private String secret;

    public boolean isEnabled() {
        return enabled && StringUtils.hasText(secret);
    }

    public boolean isConfigured() {
        return StringUtils.hasText(secret);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
