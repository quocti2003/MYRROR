package com.mirror.product.controller;

import com.mirror.product.dto.FileUploadResponse;
import com.mirror.product.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    /**
     * Test S3 connection by listing buckets
     */
    @GetMapping("/buckets")
    public ResponseEntity<?> listBuckets() {
        try {
            List<Bucket> buckets = s3Service.listBuckets();
            // Extract bucket names for JSON serialization
            List<String> bucketNames = buckets.stream()
                    .map(Bucket::name)
                    .toList();
            return ResponseEntity.ok(bucketNames);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error connecting to S3: " + e.getMessage());
        }
    }

    /**
     * List objects in a specific bucket
     */
    @GetMapping("/buckets/{bucketName}/objects")
    public ResponseEntity<?> listObjects(@PathVariable String bucketName, 
                                       @RequestParam(required = false) String prefix) {
        try {
            List<S3Object> objects = s3Service.listObjects(bucketName, prefix);
            return ResponseEntity.ok(objects);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error listing objects: " + e.getMessage());
        }
    }

    /**
     * Upload file to S3 and return downloadable URL
     */
    @PostMapping("/buckets/{bucketName}/upload")
    public ResponseEntity<?> uploadFile(@PathVariable String bucketName,
                                      @RequestParam("file") MultipartFile file,
                                      @RequestParam(required = false) String key) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            String fileName = key != null ? key : UUID.randomUUID() + "_" + file.getOriginalFilename();
            
            FileUploadResponse response = s3Service.uploadFile(
                bucketName, 
                fileName, 
                file.getInputStream(), 
                file.getSize(), 
                file.getContentType()
            );

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error uploading file: " + e.getMessage());
        }
    }

    /**
     * Check if file exists in S3
     */
    @GetMapping("/buckets/{bucketName}/exists/{key}")
    public ResponseEntity<?> checkFileExists(@PathVariable String bucketName, 
                                           @PathVariable String key) {
        try {
            boolean exists = s3Service.fileExists(bucketName, key);
            return ResponseEntity.ok().body(String.format("File %s in bucket %s exists: %s", key, bucketName, exists));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error checking file existence: " + e.getMessage());
        }
    }

    /**
     * Delete file from S3
     */
    @DeleteMapping("/buckets/{bucketName}/objects/{key}")
    public ResponseEntity<?> deleteFile(@PathVariable String bucketName, 
                                      @PathVariable String key) {
        try {
            s3Service.deleteFile(bucketName, key);
            return ResponseEntity.ok().body(String.format("File %s deleted from bucket %s", key, bucketName));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting file: " + e.getMessage());
        }
    }

    /**
     * Generate download URL for existing file
     */
    @GetMapping("/buckets/{bucketName}/download/{key}")
    public ResponseEntity<?> generateDownloadUrl(@PathVariable String bucketName, 
                                               @PathVariable String key,
                                               @RequestParam(defaultValue = "24") int hoursValid) {
        try {
            if (!s3Service.fileExists(bucketName, key)) {
                return ResponseEntity.notFound().build();
            }
            
            String downloadUrl = s3Service.generatePresignedUrl(bucketName, key, Duration.ofHours(hoursValid));
            
            return ResponseEntity.ok().body(String.format("Download URL (valid for %d hours): %s", hoursValid, downloadUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating download URL: " + e.getMessage());
        }
    }

    /**
     * Simple health check for S3 connection
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            List<Bucket> buckets = s3Service.listBuckets();
            return ResponseEntity.ok().body(String.format("S3 connection OK. Found %d buckets.", buckets.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("S3 connection failed: " + e.getMessage());
        }
    }
}