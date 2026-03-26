package com.mirror.product.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DotenvConfig implements EnvironmentPostProcessor {
    
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        loadEnv(environment);
    }
    
    private void loadEnv(ConfigurableEnvironment environment) {
        try {
            File envLocal = new File(".env.local");
            File envFile = new File(".env");
            
            Dotenv dotenv;
            
            if (envLocal.exists()) {
                log.info("Loading environment variables from .env.local");
                dotenv = Dotenv.configure()
                    .filename(".env.local")
                    .ignoreIfMissing()
                    .load();
            } else if (envFile.exists()) {
                log.info("Loading environment variables from .env");
                dotenv = Dotenv.configure()
                    .filename(".env")
                    .ignoreIfMissing()
                    .load();
            } else {
                log.warn("No .env or .env.local file found, using system environment variables");
                return;
            }
            
            Map<String, Object> properties = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                
                properties.put(key, value);
                System.setProperty(key, value);
                
                if (!key.contains("SECRET") && !key.contains("PASSWORD") && !key.contains("KEY")) {
                    log.debug("Loaded environment variable: {} = {}", key, value);
                } else {
                    log.debug("Loaded sensitive environment variable: {}", key);
                }
            });
            
            environment.getPropertySources().addLast(new MapPropertySource("dotenv", properties));
            log.info("Environment variables loaded successfully");
            
        } catch (Exception e) {
            log.error("Failed to load environment variables: {}", e.getMessage());
        }
    }
}