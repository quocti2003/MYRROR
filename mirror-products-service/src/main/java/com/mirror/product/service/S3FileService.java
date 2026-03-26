package com.mirror.product.service;

import com.mirror.product.entity.S3File;
import com.mirror.product.repository.S3FileRepository;
import com.mirror.product.dto.FileUploadResponse;
import com.mirror.product.util.S3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class S3FileService extends BaseService<S3File, String> {
    
    private final S3FileRepository s3FileRepository;
    private final S3Service s3Service;
    private final S3FolderService s3FolderService;
    private final software.amazon.awssdk.services.s3.S3Client s3Client;
    
    public S3FileService(S3FileRepository s3FileRepository, S3Service s3Service, S3FolderService s3FolderService, software.amazon.awssdk.services.s3.S3Client s3Client) {
        super(s3FileRepository);
        this.s3FileRepository = s3FileRepository;
        this.s3Service = s3Service;
        this.s3FolderService = s3FolderService;
        this.s3Client = s3Client;
    }
    
    /**
     * Upload file to S3 and save metadata to database
     */
    public FileUploadResponse uploadFile(String bucketName, MultipartFile file, String description) throws IOException {
        return uploadFileToFolder(bucketName, null, file, description);
    }
    
    /**
     * Upload file to specific folder in S3
     */
    public FileUploadResponse uploadFileToFolder(String bucketName, String folderPath, MultipartFile file, String description) throws IOException {
        // Validate and create folder if specified
        String s3Key;
        if (folderPath != null && !folderPath.trim().isEmpty()) {
            // Ensure folder exists
            s3FolderService.createFolderPath(bucketName, folderPath);
            
            // Generate S3 key with folder path
            s3Key = S3Utils.generateSafeS3Key(file.getOriginalFilename(), folderPath);
        } else {
            // Upload to root
            s3Key = UUID.randomUUID() + "_" + file.getOriginalFilename();
        }
        
        // Upload to S3
        FileUploadResponse s3Response = s3Service.uploadFile(
            bucketName, 
            s3Key, 
            file.getInputStream(), 
            file.getSize(), 
            file.getContentType()
        );
        
        // Save metadata to database
        S3File s3File = S3File.builder()
                .originalFilename(file.getOriginalFilename())
                .s3Key(s3Key)
                .s3Bucket(bucketName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .eTag(s3Response.getETag())
                .description(description)
                .folderPath(folderPath) // Save folder path for easy querying
                .build();
        
        S3File savedFile = save(s3File);
        
        log.info("File uploaded and saved: {} with ID: {}", file.getOriginalFilename(), savedFile.getId());
        
        return FileUploadResponse.builder()
                .bucketName(bucketName)
                .key(s3Key)
                .eTag(s3Response.getETag())
                .downloadUrl("/api/files/" + savedFile.getId()) // Regular download URL
                .streamUrl("/api/files/" + savedFile.getId() + "/stream") // Stream URL for direct viewing
                .publicUrl(s3Response.getPublicUrl()) // Direct S3 public URL
                .message("File uploaded successfully with ID: " + savedFile.getId())
                .build();
    }
    
    /**
     * Get file content from S3 by fileId
     */
    public InputStream getFileContent(String fileId) {
        S3File s3File = findActiveById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));
        
        log.info("Downloading file: {} from S3", s3File.getOriginalFilename());
        return s3Service.downloadFile(s3File.getS3Bucket(), s3File.getS3Key());
    }
    
    /**
     * Get file metadata by fileId
     */
    public S3File getFileMetadata(String fileId) {
        return findActiveById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));
    }
    
    /**
     * Delete file from both S3 and database
     */
    @Override
    protected void validateBeforeDelete(String fileId) {
        // Có thể thêm validation nếu cần
        log.info("Validating delete for file ID: {}", fileId);
    }
    
    public void deleteFile(String fileId) {
        S3File s3File = findActiveById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));
        
        try {
            // Delete from S3
            s3Service.deleteFile(s3File.getS3Bucket(), s3File.getS3Key());
            
            // Soft delete from database
            softDeleteById(fileId);
            
            log.info("File deleted successfully: {}", fileId);
        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage());
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    @Override
    public S3File update(String fileId, S3File entity) {
        S3File existingFile = findActiveById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));
        
        // Chỉ cho phép update một số field
        if (entity.getDescription() != null) {
            existingFile.setDescription(entity.getDescription());
        }
        
        return save(existingFile);
    }
    
    /**
     * Get all files in a specific folder
     */
    public List<S3File> getFilesInFolder(String bucketName, String folderPath) {
        return s3FileRepository.findByS3BucketAndFolderPathAndDeletedFalse(bucketName, folderPath);
    }
    
    /**
     * Move file to different folder
     */
    public FileUploadResponse moveFileToFolder(String fileId, String newFolderPath) {
        S3File s3File = findActiveById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));
        
        try {
            // Create new folder if not exists
            if (newFolderPath != null && !newFolderPath.trim().isEmpty()) {
                s3FolderService.createFolderPath(s3File.getS3Bucket(), newFolderPath);
            }
            
            // Generate new S3 key
            String newS3Key = S3Utils.generateSafeS3Key(s3File.getOriginalFilename(), newFolderPath);
            
            // Copy object to new location
            S3Utils.copyObject(s3Client, s3File.getS3Bucket(), s3File.getS3Key(), s3File.getS3Bucket(), newS3Key);
            
            // Delete old object
            S3Utils.deleteObject(s3Client, s3File.getS3Bucket(), s3File.getS3Key());
            
            // Update database
            s3File.setS3Key(newS3Key);
            s3File.setFolderPath(newFolderPath);
            S3File updatedFile = save(s3File);
            
            log.info("File moved successfully: {} to folder: {}", fileId, newFolderPath);
            
            return FileUploadResponse.builder()
                    .bucketName(s3File.getS3Bucket())
                    .key(newS3Key)
                    .downloadUrl("/api/files/" + updatedFile.getId())
                    .streamUrl("/api/files/" + updatedFile.getId() + "/stream")
                    .message("File moved to folder: " + newFolderPath)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error moving file to folder: {}", e.getMessage());
            throw new RuntimeException("Failed to move file to folder", e);
        }
    }
    
}