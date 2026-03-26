package com.mirror.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolderRequest {
    
    @NotBlank(message = "Bucket name is required")
    private String bucketName;
    
    @NotBlank(message = "Folder path is required")
    @Pattern(regexp = "^[a-zA-Z0-9/_.-]+$", message = "Folder path contains invalid characters")
    private String folderPath;
    
    // For move/rename operations
    private String newFolderPath;
}