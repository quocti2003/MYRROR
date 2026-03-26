package com.mirror.product.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadResponse {
    private String bucketName;
    private String key;
    private String eTag;
    private String downloadUrl;
    private String streamUrl;
    private String publicUrl;  // Direct S3 public URL
    private String message;
}