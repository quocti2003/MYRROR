package com.mirror.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderResponse {
    
    private String folderPath;
    private String bucketName;
    private long totalSize;
    private int fileCount;
    private int subfolderCount;
    private List<String> subfolders;
    private List<FileInfo> files;
    private String message;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private String key;
        private long size;
        private Instant lastModified;
        private String eTag;
        private String contentType;
        
        public static FileInfo fromS3Object(S3Object s3Object) {
            return FileInfo.builder()
                    .key(s3Object.key())
                    .size(s3Object.size())
                    .lastModified(s3Object.lastModified())
                    .eTag(s3Object.eTag())
                    .build();
        }
    }
}