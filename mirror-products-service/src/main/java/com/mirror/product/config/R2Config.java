package com.mirror.product.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

import java.net.URI;

/**
 * Cloudflare R2 Configuration
 *
 * R2 is S3-compatible, so we use the AWS SDK with custom endpoint.
 * This config is only activated when cloudflare.r2.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "cloudflare.r2.enabled", havingValue = "true")
@Getter
public class R2Config {

    @Value("${cloudflare.r2.access-key}")
    private String accessKey;

    @Value("${cloudflare.r2.secret-key}")
    private String secretKey;

    @Value("${cloudflare.r2.account-id}")
    private String accountId;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    // Public URL for accessing files (R2.dev domain or custom domain)
    @Value("${cloudflare.r2.public-url-base}")
    private String publicUrlBase;

    @Bean(name = "r2Client")
    public S3Client r2Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", accountId);

        return S3Client.builder()
                .region(Region.of("auto")) // R2 uses "auto" region
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean(name = "r2Presigner")
    public S3Presigner r2Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", accountId);

        return S3Presigner.builder()
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    /**
     * Generate public URL for a file in R2
     */
    public String getPublicUrl(String key) {
        String base = publicUrlBase.endsWith("/") ? publicUrlBase : publicUrlBase + "/";
        return base + key;
    }
}
