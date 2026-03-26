package com.mirror.product.service;

import com.mirror.product.config.R2Config;
import com.mirror.product.dto.FileUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Cloudflare R2 Service
 *
 * Handles file operations with Cloudflare R2 storage.
 * Only activated when cloudflare.r2.enabled=true
 */
@Service
@ConditionalOnProperty(name = "cloudflare.r2.enabled", havingValue = "true")
@Slf4j
public class R2Service {

    private final S3Client r2Client;
    private final S3Presigner r2Presigner;
    private final R2Config r2Config;

    public R2Service(
            @Qualifier("r2Client") S3Client r2Client,
            @Qualifier("r2Presigner") S3Presigner r2Presigner,
            R2Config r2Config) {
        this.r2Client = r2Client;
        this.r2Presigner = r2Presigner;
        this.r2Config = r2Config;
    }

    /**
     * Upload file to R2 and return public URL
     */
    public FileUploadResponse uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        try {
            String bucketName = r2Config.getBucketName();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            PutObjectResponse response = r2Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(inputStream, contentLength));

            String publicUrl = r2Config.getPublicUrl(key);

            log.info("Successfully uploaded file to R2: {}/{}", bucketName, key);

            return FileUploadResponse.builder()
                    .bucketName(bucketName)
                    .key(key)
                    .eTag(response.eTag())
                    .publicUrl(publicUrl)
                    .downloadUrl(publicUrl) // R2 public URL is the download URL
                    .build();
        } catch (Exception e) {
            log.error("Error uploading file to R2: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to R2", e);
        }
    }

    /**
     * Upload file with auto-generated key
     */
    public FileUploadResponse uploadFile(String folderPath, String originalFilename, InputStream inputStream, long contentLength, String contentType) {
        String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = normalizeFolderPath(folderPath) + UUID.randomUUID() + "_" + safeFilename;
        return uploadFile(key, inputStream, contentLength, contentType);
    }

    /**
     * Generate presigned URL for direct upload from frontend
     */
    public String generateUploadUrl(String key, String contentType, Duration expiration) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = r2Presigner.presignPutObject(presignRequest);
            String url = presignedRequest.url().toString();

            log.info("Generated presigned upload URL for key: {}", key);
            return url;
        } catch (Exception e) {
            log.error("Error generating presigned upload URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate presigned upload URL", e);
        }
    }

    /**
     * Generate presigned URL for download
     */
    public String generateDownloadUrl(String key, Duration expiration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = r2Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Error generating presigned download URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate presigned download URL", e);
        }
    }

    /**
     * Get public URL for a file
     */
    public String getPublicUrl(String key) {
        return r2Config.getPublicUrl(key);
    }

    /**
     * Download file from R2
     */
    public InputStream downloadFile(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(key)
                    .build();

            log.info("Successfully downloaded file from R2: {}", key);
            return r2Client.getObject(getObjectRequest);
        } catch (Exception e) {
            log.error("Error downloading file from R2: {}", e.getMessage());
            throw new RuntimeException("Failed to download file from R2", e);
        }
    }

    /**
     * Delete file from R2
     */
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(key)
                    .build();

            r2Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted file from R2: {}", key);
        } catch (Exception e) {
            log.error("Error deleting file from R2: {}", e.getMessage());
            throw new RuntimeException("Failed to delete file from R2", e);
        }
    }

    /**
     * Check if file exists in R2
     */
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(key)
                    .build();

            r2Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking file existence in R2: {}", e.getMessage());
            throw new RuntimeException("Failed to check file existence in R2", e);
        }
    }

    /**
     * List objects in R2 bucket with prefix
     */
    public List<S3Object> listObjects(String prefix) {
        try {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(r2Config.getBucketName());

            if (prefix != null && !prefix.isEmpty()) {
                requestBuilder.prefix(prefix);
            }

            ListObjectsV2Response response = r2Client.listObjectsV2(requestBuilder.build());
            log.info("Found {} objects in R2 with prefix: {}", response.contents().size(), prefix);
            return response.contents();
        } catch (Exception e) {
            log.error("Error listing objects in R2: {}", e.getMessage());
            throw new RuntimeException("Failed to list objects in R2", e);
        }
    }

    private String normalizeFolderPath(String folderPath) {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            return "";
        }
        String normalized = folderPath.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        return normalized;
    }
}
