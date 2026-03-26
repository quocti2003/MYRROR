package com.mirror.product.service;

import com.mirror.product.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    /**
     * List all buckets
     */
    public List<Bucket> listBuckets() {
        try {
            ListBucketsResponse response = s3Client.listBuckets();
            log.info("Found {} buckets", response.buckets().size());
            return response.buckets();
        } catch (Exception e) {
            log.error("Error listing buckets: {}", e.getMessage());
            throw new RuntimeException("Failed to list buckets", e);
        }
    }

    /**
     * Upload file to S3 and return downloadable URL
     */
    public FileUploadResponse uploadFile(String bucketName, String key, InputStream inputStream, long contentLength, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
//                    .acl(software.amazon.awssdk.services.s3.model.ObjectCannedACL.PUBLIC_READ) // Make object public
                    .build();

            PutObjectResponse response = s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(inputStream, contentLength));
            
            // Generate presigned URL for download
            String downloadUrl = generatePresignedUrl(bucketName, key, Duration.ofHours(24));
            
            // Generate direct public S3 URL (since we set ACL to public-read)
            String publicUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", 
                    bucketName, 
                    "ap-southeast-1", // TODO: Get from config
                    key);
            
            log.info("Successfully uploaded file to S3: {}/{}", bucketName, key);
            
            return FileUploadResponse.builder()
                    .bucketName(bucketName)
                    .key(key)
                    .eTag(response.eTag())
                    .downloadUrl(downloadUrl)
                    .publicUrl(publicUrl)
                    .build();
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    /**
     * Download file from S3
     */
    public InputStream downloadFile(String bucketName, String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            log.info("Successfully downloaded file from S3: {}/{}", bucketName, key);
            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            log.error("Error downloading file from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    /**
     * Delete file from S3
     */
    public void deleteFile(String bucketName, String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted file from S3: {}/{}", bucketName, key);
        } catch (Exception e) {
            log.error("Error deleting file from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    /**
     * Check if file exists in S3
     */
    public boolean fileExists(String bucketName, String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking file existence in S3: {}", e.getMessage());
            throw new RuntimeException("Failed to check file existence in S3", e);
        }
    }

    /**
     * List objects in bucket
     */
    public List<S3Object> listObjects(String bucketName, String prefix) {
        try {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName);
            
            if (prefix != null && !prefix.isEmpty()) {
                requestBuilder.prefix(prefix);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            log.info("Found {} objects in bucket {}", response.contents().size(), bucketName);
            return response.contents();
        } catch (Exception e) {
            log.error("Error listing objects in S3: {}", e.getMessage());
            throw new RuntimeException("Failed to list objects in S3", e);
        }
    }

    /**
     * Generate presigned URL for downloading file
     */
    public String generatePresignedUrl(String bucketName, String key, Duration expiration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();
            
            log.info("Generated presigned URL for {}/{}", bucketName, key);
            return url;
        } catch (Exception e) {
            log.error("Error generating presigned URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Generate download URL with default 24 hour expiration
     */
    public String generateDownloadUrl(String bucketName, String key) {
        return generatePresignedUrl(bucketName, key, Duration.ofHours(24));
    }
    
    /**
     * Copy object from one location to another within S3
     */
    public void copyObject(String sourceBucket, String sourceKey, String destBucket, String destKey) {
        com.mirror.product.util.S3Utils.copyObject(s3Client, sourceBucket, sourceKey, destBucket, destKey);
    }
}