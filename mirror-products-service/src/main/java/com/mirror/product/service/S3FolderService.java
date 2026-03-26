package com.mirror.product.service;

import com.mirror.product.util.S3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3FolderService {

    private final S3Client s3Client;

    /**
     * Tạo folder mới trên S3 (thực chất là tạo một empty object với key kết thúc bằng "/")
     */
    public void createFolder(String bucketName, String folderPath) {
        String normalizedPath = S3Utils.normalizeFolderPath(folderPath);
        
        // Kiểm tra folder đã tồn tại chưa
        if (folderExists(bucketName, normalizedPath)) {
            throw new RuntimeException("Folder already exists: " + normalizedPath);
        }
        
        S3Utils.createEmptyObject(s3Client, bucketName, normalizedPath);
    }

    /**
     * Kiểm tra folder có tồn tại không
     */
    public boolean folderExists(String bucketName, String folderPath) {
        String normalizedPath = S3Utils.normalizeFolderPath(folderPath);
        List<S3Object> objects = S3Utils.listObjects(s3Client, bucketName, normalizedPath, null);
        return !objects.isEmpty();
    }

    /**
     * List tất cả folders trong bucket hoặc trong một folder cụ thể
     */
    public List<String> listFolders(String bucketName, String parentFolderPath) {
        return S3Utils.listCommonPrefixes(s3Client, bucketName, parentFolderPath);
    }

    /**
     * List tất cả files trong một folder
     */
    public List<S3Object> listFilesInFolder(String bucketName, String folderPath) {
        String prefix = S3Utils.normalizeFolderPath(folderPath);
        List<S3Object> objects = S3Utils.listObjects(s3Client, bucketName, prefix, "/");
        
        // Filter out folder objects (keys ending with "/")
        return objects.stream()
                .filter(obj -> !obj.key().endsWith("/"))
                .collect(Collectors.toList());
    }

    /**
     * Xóa folder và tất cả contents bên trong
     */
    public void deleteFolder(String bucketName, String folderPath) {
        String normalizedPath = S3Utils.normalizeFolderPath(folderPath);
        
        // List tất cả objects trong folder
        List<S3Object> objects = S3Utils.listObjects(s3Client, bucketName, normalizedPath, null);
        
        if (objects.isEmpty()) {
            throw new RuntimeException("Folder not found: " + folderPath);
        }
        
        // Extract keys và delete
        List<String> keys = objects.stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
        
        S3Utils.deleteObjects(s3Client, bucketName, keys);
    }

    /**
     * Rename/Move folder
     */
    public void moveFolder(String bucketName, String oldFolderPath, String newFolderPath) {
        String oldNormalizedPath = S3Utils.normalizeFolderPath(oldFolderPath);
        String newNormalizedPath = S3Utils.normalizeFolderPath(newFolderPath);
        
        // Kiểm tra folder cũ có tồn tại không
        if (!folderExists(bucketName, oldNormalizedPath)) {
            throw new RuntimeException("Source folder not found: " + oldFolderPath);
        }
        
        // Kiểm tra folder mới đã tồn tại chưa
        if (folderExists(bucketName, newNormalizedPath)) {
            throw new RuntimeException("Destination folder already exists: " + newFolderPath);
        }
        
        // List tất cả objects trong folder cũ
        List<S3Object> objects = S3Utils.listObjects(s3Client, bucketName, oldNormalizedPath, null);
        
        // Copy và delete từng object
        for (S3Object obj : objects) {
            String oldKey = obj.key();
            String newKey = oldKey.replace(oldNormalizedPath, newNormalizedPath);
            
            S3Utils.copyObject(s3Client, bucketName, oldKey, bucketName, newKey);
            S3Utils.deleteObject(s3Client, bucketName, oldKey);
        }
        
        log.info("Successfully moved folder from {}/{} to {}/{}", 
                bucketName, oldFolderPath, bucketName, newFolderPath);
    }

    /**
     * Tạo full folder path nếu chưa tồn tại
     */
    public void createFolderPath(String bucketName, String fullPath) {
        try {
            String[] pathParts = fullPath.split("/");
            StringBuilder currentPath = new StringBuilder();
            
            for (String part : pathParts) {
                if (!part.isEmpty()) {
                    currentPath.append(part).append("/");
                    String folderPath = currentPath.toString();
                    
                    if (!folderExists(bucketName, folderPath)) {
                        createFolder(bucketName, folderPath);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error creating folder path {}/{}: {}", bucketName, fullPath, e.getMessage());
            throw new RuntimeException("Failed to create folder path: " + fullPath, e);
        }
    }

    /**
     * Get folder info (size, file count, etc.)
     */
    public FolderInfo getFolderInfo(String bucketName, String folderPath) {
        String normalizedPath = S3Utils.normalizeFolderPath(folderPath);
        List<S3Object> objects = S3Utils.listObjects(s3Client, bucketName, normalizedPath, null);
        
        long totalSize = 0;
        int fileCount = 0;
        int folderCount = 0;
        
        for (S3Object obj : objects) {
            if (obj.key().endsWith("/")) {
                folderCount++;
            } else {
                fileCount++;
                totalSize += obj.size();
            }
        }
        
        return FolderInfo.builder()
                .folderPath(folderPath)
                .totalSize(totalSize)
                .fileCount(fileCount)
                .subfolderCount(folderCount - 1) // Subtract 1 for the folder itself
                .build();
    }


    // Inner class for folder information
    @lombok.Builder
    @lombok.Data
    public static class FolderInfo {
        private String folderPath;
        private long totalSize;
        private int fileCount;
        private int subfolderCount;
    }
}