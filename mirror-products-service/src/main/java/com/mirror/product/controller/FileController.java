package com.mirror.product.controller;

import com.mirror.product.dto.FileUploadResponse;
import com.mirror.product.entity.S3File;
import com.mirror.product.service.S3FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final S3FileService s3FileService;

    /**
     * Upload file to S3 and save metadata
     * POST /api/files/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "mirror-storage") String bucketName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String folderPath) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            FileUploadResponse response = s3FileService.uploadFileToFolder(bucketName, folderPath, file, description);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error uploading file: " + e.getMessage());
        }
    }

    /**
     * Download file by ID - serve as binary data
     * GET /api/files/{fileId}
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<?> downloadFile(@PathVariable String fileId) {
        try {
            // Get file metadata
            S3File fileMetadata = s3FileService.getFileMetadata(fileId);
            
            // Get file content from S3
            InputStream fileContent = s3FileService.getFileContent(fileId);
            
            // Set appropriate headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(fileMetadata.getContentType()));
            
            // Set Content-Disposition to attachment for download
            headers.set("Content-Disposition", "attachment; filename=\"" + fileMetadata.getOriginalFilename() + "\"");
            
            // For images, set cache headers
            if (fileMetadata.getContentType().startsWith("image/")) {
                headers.setCacheControl("max-age=3600"); // Cache for 1 hour
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(fileContent));
                    
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error downloading file: " + e.getMessage());
        }
    }

    /**
     * Stream file by ID - serve as binary data (Public URL)
     * GET /api/files/{fileId}/stream
     */
    @GetMapping("/{fileId}/stream")
    public ResponseEntity<InputStreamResource> streamFile(@PathVariable String fileId) {
        try {
            // Get file metadata from database
            S3File fileMetadata = s3FileService.getFileMetadata(fileId);
            
            // Get file content stream from S3
            InputStream fileContent = s3FileService.getFileContent(fileId);

            // Set appropriate headers for streaming
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(fileMetadata.getContentType()));
            headers.setContentLength(fileMetadata.getFileSize());
            
            // Set Content-Disposition to inline so browsers display directly
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline");
            
            // Add cache headers for better performance
            if (fileMetadata.getContentType().startsWith("image/")) {
                headers.setCacheControl("public, max-age=3600"); // Cache for 1 hour
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(fileContent));

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get file metadata by ID
     * GET /api/files/{fileId}/info
     */
    @GetMapping("/{fileId}/info")
    public ResponseEntity<?> getFileInfo(@PathVariable String fileId) {
        try {
            S3File fileMetadata = s3FileService.getFileMetadata(fileId);
            return ResponseEntity.ok(fileMetadata);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error getting file info: " + e.getMessage());
        }
    }

    /**
     * Get all files metadata
     * GET /api/files
     */
    @GetMapping
    public ResponseEntity<?> getAllFiles() {
        try {
            List<S3File> files = s3FileService.findAllActive();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error getting files: " + e.getMessage());
        }
    }

    /**
     * Delete file by ID
     * DELETE /api/files/{fileId}
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable String fileId) {
        try {
            s3FileService.deleteFile(fileId);
            return ResponseEntity.ok().body("File deleted successfully: " + fileId);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting file: " + e.getMessage());
        }
    }

    /**
     * Update file description
     * PUT /api/files/{fileId}
     */
    @PutMapping("/{fileId}")
    public ResponseEntity<?> updateFile(@PathVariable String fileId, 
                                      @RequestParam String description) {
        try {
            S3File updateFile = S3File.builder()
                    .description(description)
                    .build();
                    
            S3File updatedFile = s3FileService.update(fileId, updateFile);
            return ResponseEntity.ok(updatedFile);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating file: " + e.getMessage());
        }
    }
    
    /**
     * Get files in a specific folder
     * GET /api/files/folder
     */
    @GetMapping("/folder")
    public ResponseEntity<?> getFilesInFolder(
            @RequestParam String bucketName,
            @RequestParam String folderPath) {
        try {
            List<S3File> files = s3FileService.getFilesInFolder(bucketName, folderPath);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error getting files in folder: " + e.getMessage());
        }
    }
    
    /**
     * Move file to different folder
     * PUT /api/files/{fileId}/move
     */
    @PutMapping("/{fileId}/move")
    public ResponseEntity<?> moveFileToFolder(
            @PathVariable String fileId,
            @RequestParam String newFolderPath) {
        try {
            FileUploadResponse response = s3FileService.moveFileToFolder(fileId, newFolderPath);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error moving file: " + e.getMessage());
        }
    }
}