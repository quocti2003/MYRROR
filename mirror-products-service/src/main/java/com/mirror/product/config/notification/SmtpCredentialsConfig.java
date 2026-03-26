package com.mirror.product.config.notification;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class SmtpCredentialsConfig {

    private static final Logger log = LoggerFactory.getLogger(SmtpCredentialsConfig.class);

    private final MailProperties mailProperties;
    private final ResourceLoader resourceLoader;
    private final String credentialsLocation;

    public SmtpCredentialsConfig(
            MailProperties mailProperties,
            ResourceLoader resourceLoader,
            @Value("${app.notification.smtp.credentials-path:}") String credentialsLocation) {
        this.mailProperties = mailProperties;
        this.resourceLoader = resourceLoader;
        this.credentialsLocation = credentialsLocation;
    }

    @PostConstruct
    public void loadCredentialsIfNecessary() {
        if (!StringUtils.hasText(credentialsLocation)) {
            log.debug("SMTP credentials location not configured; skipping CSV load");
            return;
        }
        if (StringUtils.hasText(mailProperties.getUsername()) && StringUtils.hasText(mailProperties.getPassword())) {
            log.debug("SMTP credentials already provided via properties; skipping CSV load");
            return;
        }

        try {
            Resource resource = resourceLoader.getResource(credentialsLocation);
            if (!resource.exists()) {
                log.warn("SMTP credentials resource {} not found", credentialsLocation);
                return;
            }

            Map<String, String> values = readCsv(resource);
            String username = values.getOrDefault("smtp user name", values.get("smtp_username"));
            String password = values.getOrDefault("smtp password", values.get("smtp_password"));

            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                log.warn("SMTP credentials CSV {} did not contain required columns", credentialsLocation);
                return;
            }

            mailProperties.setUsername(username.trim());
            mailProperties.setPassword(password.trim());
            log.info("Loaded SMTP credentials from {}", credentialsLocation);
        } catch (Exception ex) {
            log.warn("Failed to load SMTP credentials from {}", credentialsLocation, ex);
        }
    }

    private Map<String, String> readCsv(Resource resource) throws Exception {
        Map<String, String> result = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            String valuesLine = reader.readLine();
            if (headerLine == null || valuesLine == null) {
                return result;
            }
            String[] headers = headerLine.replace("\ufeff", "").split(",");
            String[] values = valuesLine.split(",");
            for (int i = 0; i < headers.length && i < values.length; i++) {
                result.put(headers[i].trim().toLowerCase(), values[i].trim());
            }
        }
        return result;
    }
}
