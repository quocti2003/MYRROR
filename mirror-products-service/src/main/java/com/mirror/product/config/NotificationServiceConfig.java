package com.mirror.product.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(NotificationServiceProperties.class)
public class NotificationServiceConfig {

    @Bean
    @ConditionalOnProperty(prefix = "notification.service", name = "enabled", havingValue = "true")
    public RestClient notificationRestClient(NotificationServiceProperties properties) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException("notification.service.base-url must be configured when notification service is enabled");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
