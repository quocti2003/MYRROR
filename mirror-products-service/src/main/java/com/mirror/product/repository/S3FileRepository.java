package com.mirror.product.repository;

import com.mirror.product.entity.S3File;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface S3FileRepository extends BaseRepository<S3File, String> {
    
    @Override
    @Query("SELECT f FROM S3File f WHERE f.id = :id AND f.isActive = true AND f.isDeleted = false")
    Optional<S3File> findActiveById(@Param("id") String id);
    
    @Override
    @Query("SELECT COUNT(f) > 0 FROM S3File f WHERE f.id = :id AND f.isActive = true AND f.isDeleted = false")
    boolean existsActiveById(@Param("id") String id);
    
    @Query("SELECT f FROM S3File f WHERE f.s3Key = :s3Key AND f.isActive = true AND f.isDeleted = false")
    Optional<S3File> findActiveByS3Key(@Param("s3Key") String s3Key);
    
    @Query("SELECT f FROM S3File f WHERE f.originalFilename = :filename AND f.isActive = true AND f.isDeleted = false")
    List<S3File> findActiveByOriginalFilename(@Param("filename") String filename);
    
    @Query("SELECT f FROM S3File f WHERE f.contentType LIKE :contentType AND f.isActive = true AND f.isDeleted = false")
    List<S3File> findActiveByContentType(@Param("contentType") String contentType);
    
    @Query("SELECT f FROM S3File f WHERE f.s3Bucket = :bucketName AND f.folderPath = :folderPath AND f.isActive = true AND f.isDeleted = false")
    List<S3File> findByS3BucketAndFolderPathAndDeletedFalse(@Param("bucketName") String bucketName, @Param("folderPath") String folderPath);
}