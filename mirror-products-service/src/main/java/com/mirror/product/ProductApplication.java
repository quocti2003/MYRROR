package com.mirror.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.mirror.product.config.NotificationServiceProperties;
import com.mirror.product.config.notification.NotificationProperties;
import com.mirror.product.config.notification.TurnstileProperties;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({
    NotificationProperties.class,
    TurnstileProperties.class,
    NotificationServiceProperties.class
})
public class ProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductApplication.class, args);
	}

}
