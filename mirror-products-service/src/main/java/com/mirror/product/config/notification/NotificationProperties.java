package com.mirror.product.config.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    @NotBlank
    private String defaultSender;

    private String defaultSenderName;

    @NotNull
    private final Templates templates = new Templates();

    public String getDefaultSender() {
        return defaultSender;
    }

    public void setDefaultSender(String defaultSender) {
        this.defaultSender = defaultSender;
    }

    public String getDefaultSenderName() {
        return defaultSenderName;
    }

    public void setDefaultSenderName(String defaultSenderName) {
        this.defaultSenderName = defaultSenderName;
    }

    public Templates getTemplates() {
        return templates;
    }

    public static class Templates {

        private Map<String, String> ids = new HashMap<>();

        public Map<String, String> getIds() {
            return ids;
        }

        public void setIds(Map<String, String> ids) {
            this.ids = ids;
        }
    }
}
