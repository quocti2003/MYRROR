package com.mirror.product.controller;

import com.mirror.product.dto.FolderRequest;
import com.mirror.product.dto.FolderResponse;
import com.mirror.product.service.S3FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@Slf4j
public class S3FolderController {

    private final S3FolderService s3FolderService;

    /**
     * Tạo folder mới
     */
    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(@Valid @RequestBody FolderRequest request) {
        try {
            s3FolderService.createFolder(request.getBucketName(), request.getFolderPath());
            
            FolderResponse response = FolderResponse.builder()
                    .folderPath(request.getFolderPath())
                    .bucketName(request.getBucketName())
                    .message("Folder created successfully")
                    .build();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Error creating folder: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(FolderResponse.builder()
                            .message("Error creating folder: " + e.getMessage())
                            .build());
        }
    }

    /**
     * List folders trong bucket hoặc trong folder cụ thể
     */
    @GetMapping
    public ResponseEntity<FolderResponse> listFolders(
            @RequestParam String bucketName,
            @RequestParam(required = false) String parentFolder) {
        try {
            List<String> folders = s3FolderService.listFolders(bucketName, parentFolder);
            
            FolderResponse response = FolderResponse.builder()
                    .bucketName(bucketName)
                    .folderPath(parentFolder)
                    .subfolders(folders)
                    .subfolderCount(folders.size())
                    .message("Folders retrieved successfully")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error listing folders: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(FolderResponse.builder()
                            .message("Error listing folders: " + e.getMessage())
                            .build());
        }
    }

    /**
     * List files trong folder
     */
    @GetMapping("/files")
    public ResponseEntity<FolderResponse> listFilesInFolder(
            @RequestParam String bucketName,
            @RequestParam String folderPath) {
        try {
            List<S3Object> s3Objects = s3FolderService.listFilesInFolder(bucketName, folderPath);
            
            List<FolderResponse.FileInfo> files = s3Objects.stream()
                    .map(FolderResponse.FileInfo::fromS3Object)
                    .collect(Collectors.toList());
            
            FolderResponse response = FolderResponse.builder()
                    .bucketName(bucketName)
                    .folderPath(folderPath)
                    .files(files)
                    .fileCount(files.size())
                    .message("Files retrieved successfully")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error listing files in folder: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(FolderResponse.builder()
                            .message("Error listing files: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get folder info (size, file count, etc.)
     */
    @GetMapping("/info")
    public ResponseEntity<FolderResponse> getFolderInfo(
            @RequestParam String bucketName,
            @RequestParam String folderPath) {
        try {
            S3FolderService.FolderInfo folderInfo = s3FolderService.getFolderInfo(bucketName, folderPath);
            
            FolderResponse response = FolderResponse.builder()
                    .bucketName(bucketName)
                    .folderPath(folderInfo.getFolderPath())
                    .totalSize(folderInfo.getTotalSize())
                    .fileCount(folderInfo.getFileCount())
                    .subfolderCount(folderInfo.getSubfolderCount())
                    .message("Folder info retrieved successfully")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting folder info: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(FolderResponse.builder()
                            .message("Error getting folder info: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Check if folder exists
     */
    @GetMapping("/exists")
    public ResponseEntity<FolderResponse> checkFolderExists(
            @RequestParam String bucketName,
            @RequestParam String folderPath) {
        try {
            boolean exists = s3FolderService.folderExists(bucketName, folderPath);
            
            FolderResponse response = FolderResponse.builder()
                    .bucketName(bucketName)
                    .folderPath(folderPath)
                    .message(exists ? "Folder exists" : "Folder does not exist")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking folder existence: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(FolderResponse.builder()
                            .message("Error checking folder: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Move/rename folder
     */
    @PutMapping("/move")
    public ResponseEntity<FolderResponse> moveFolder(@Valid @RequestBody FolderRequest request) {
        try {
            if (request.getNewFolderPath() == null || request.getNewFolderPath().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(FolderResponse.builder()
                                .message("New folder path is required for move operation")
                                .build());
            }
            
            s3FolderService.moveFolder(request.getBucketName(), request.getFolderPath(), request.getNewFolderPath());
            
            FolderResponse response = FolderResponse.builder()
                    .bucketName(request.getBucketName())
                    .folderPath(request.getNewFolderPath())
                    .message("Folder moved successfully from " + request.getFolderPath() + " to " + request.getNewFolderPath())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error moving folder: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(FolderResponse.builder()
                            .message("Error moving folder: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Xóa folder và tất cả contents
     */
    @DeleteMapping
    public ResponseEntity<FolderResponse> deleteFolder(
            @RequestParam String bucketName,
            @RequestParam String folderPath) {
        try {
            s3FolderService.deleteFolder(bucketName, folderPath);
            
            FolderResponse response = FolderResponse.builder()
                    .bucketName(bucketName)
                    .folderPath(folderPath)
                    .message("Folder deleted successfully")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting folder: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(FolderResponse.builder()
                            .message("Error deleting folder: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Tạo full folder path (tạo tất cả parent folders nếu chưa tồn tại)
     */
    @PostMapping("/create-path")
    public ResponseEntity<FolderResponse> createFolderPath(@Valid @RequestBody FolderRequest request) {
        try {
            s3FolderService.createFolderPath(request.getBucketName(), request.getFolderPath());
            
            FolderResponse response = FolderResponse.builder()
                    .folderPath(request.getFolderPath())
                    .bucketName(request.getBucketName())
                    .message("Folder path created successfully")
                    .build();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Error creating folder path: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(FolderResponse.builder()
                            .message("Error creating folder path: " + e.getMessage())
                            .build());
        }
    }
}