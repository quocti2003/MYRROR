package com.mirror.product.util;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class S3Utils {

    /**
     * Normalize folder path để đảm bảo kết thúc bằng "/" và không bắt đầu bằng "/"
     */
    public static String normalizeFolderPath(String folderPath) {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            return "";
        }
        
        String normalized = folderPath.trim();
        
        // Remove leading slash if exists
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        
        // Add trailing slash if not exists
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        
        return normalized;
    }

    /**
     * Remove trailing "/" for display purposes
     */
    public static String denormalizeFolderPath(String folderPath) {
        if (folderPath == null || folderPath.isEmpty()) {
            return "";
        }
        
        if (folderPath.endsWith("/")) {
            return folderPath.substring(0, folderPath.length() - 1);
        }
        
        return folderPath;
    }

    /**
     * Check if object exists in S3
     */
    public static boolean objectExists(S3Client s3Client, String bucketName, String key) {
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
            log.error("Error checking object existence in S3: {}", e.getMessage());
            throw new RuntimeException("Failed to check object existence in S3", e);
        }
    }

    /**
     * List objects with prefix and optional delimiter
     */
    public static List<S3Object> listObjects(S3Client s3Client, String bucketName, String prefix, String delimiter) {
        try {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName);
            
            if (prefix != null && !prefix.isEmpty()) {
                requestBuilder.prefix(prefix);
            }
            
            if (delimiter != null && !delimiter.isEmpty()) {
                requestBuilder.delimiter(delimiter);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            return response.contents();
            
        } catch (Exception e) {
            log.error("Error listing objects in S3: {}", e.getMessage());
            throw new RuntimeException("Failed to list objects in S3", e);
        }
    }

    /**
     * List common prefixes (folders) with prefix and delimiter
     */
    public static List<String> listCommonPrefixes(S3Client s3Client, String bucketName, String prefix) {
        try {
            String normalizedPrefix = prefix != null ? normalizeFolderPath(prefix) : "";
            
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(normalizedPrefix)
                    .delimiter("/")
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            
            return response.commonPrefixes().stream()
                    .map(CommonPrefix::prefix)
                    .map(S3Utils::denormalizeFolderPath)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error listing common prefixes in S3: {}", e.getMessage());
            throw new RuntimeException("Failed to list common prefixes", e);
        }
    }

    /**
     * Delete multiple objects from S3
     */
    public static void deleteObjects(S3Client s3Client, String bucketName, List<String> keys) {
        try {
            if (keys.isEmpty()) {
                return;
            }
            
            List<ObjectIdentifier> objectsToDelete = keys.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .collect(Collectors.toList());
            
            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(objectsToDelete).build())
                    .build();

            DeleteObjectsResponse deleteResponse = s3Client.deleteObjects(deleteRequest);
            log.info("Successfully deleted {} objects from bucket {}", 
                    deleteResponse.deleted().size(), bucketName);
                    
        } catch (Exception e) {
            log.error("Error deleting objects from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to delete objects from S3", e);
        }
    }

    /**
     * Copy object within S3
     */
    public static void copyObject(S3Client s3Client, String sourceBucket, String sourceKey, String destBucket, String destKey) {
        try {
            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                    .sourceBucket(sourceBucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(destBucket)
                    .destinationKey(destKey)
                    .build();

            s3Client.copyObject(copyObjectRequest);
            log.info("Successfully copied object from {}/{} to {}/{}", 
                    sourceBucket, sourceKey, destBucket, destKey);
                    
        } catch (Exception e) {
            log.error("Error copying object in S3: {}", e.getMessage());
            throw new RuntimeException("Failed to copy object in S3", e);
        }
    }

    /**
     * Delete single object from S3
     */
    public static void deleteObject(S3Client s3Client, String bucketName, String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted object: {}/{}", bucketName, key);
            
        } catch (Exception e) {
            log.error("Error deleting object from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to delete object from S3", e);
        }
    }

    /**
     * Create empty object (for folder creation)
     */
    public static void createEmptyObject(S3Client s3Client, String bucketName, String key) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentLength(0L)
                    .build();

            s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.empty());
            log.info("Successfully created empty object: {}/{}", bucketName, key);
            
        } catch (Exception e) {
            log.error("Error creating empty object in S3: {}", e.getMessage());
            throw new RuntimeException("Failed to create empty object in S3", e);
        }
    }

    /**
     * Validate bucket name format
     */
    public static boolean isValidBucketName(String bucketName) {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            return false;
        }
        
        String name = bucketName.trim();
        
        // Basic validation rules for S3 bucket names
        return name.length() >= 3 && name.length() <= 63 &&
               name.matches("^[a-z0-9.-]+$") &&
               !name.startsWith(".") &&
               !name.endsWith(".") &&
               !name.contains("..");
    }

    /**
     * Validate S3 key format
     */
    public static boolean isValidS3Key(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        // S3 keys can be up to 1024 characters
        return key.length() <= 1024;
    }

    /**
     * Generate safe S3 key from filename
     */
    public static String generateSafeS3Key(String filename, String folderPath) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        
        String safeFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String normalizedFolder = folderPath != null ? normalizeFolderPath(folderPath) : "";
        
        return normalizedFolder + java.util.UUID.randomUUID() + "_" + safeFilename;
    }
}