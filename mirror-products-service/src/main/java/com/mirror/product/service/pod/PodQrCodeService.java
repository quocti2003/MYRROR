package com.mirror.product.service.pod;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mirror.product.dto.FileUploadResponse;
import com.mirror.product.dto.pod.*;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.pod.Pod;
import com.mirror.product.entity.pod.PodQrCode;
import com.mirror.product.enums.QrCodeStatus;
import com.mirror.product.exception.pod.PodNotFoundException;
import com.mirror.product.exception.pod.QrCodeNotFoundException;
import com.mirror.product.repository.MirrorProductRepository;
import com.mirror.product.repository.pod.PodQrCodeRepository;
import com.mirror.product.repository.pod.PodRepository;
import com.mirror.product.service.R2Service;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PodQrCodeService {

    private final PodQrCodeRepository qrCodeRepository;
    private final PodRepository podRepository;
    private final MirrorProductRepository productRepository;
    private final R2Service r2Service;

    @Value("${pod.qr.base-url:https://mirror.vn/q/}")
    private String qrBaseUrl;

    @Value("${pod.qr.image-size:300}")
    private int qrImageSize;

    private static final String QR_FOLDER = "qrcodes";

    private static final String SHORT_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SHORT_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Create a single QR code for a product in a POD
     */
    @Transactional
    public QrCodeResponse createQrCode(QrCodeCreateRequest request) {
        log.info("Creating QR code for POD: {} and product: {}", request.getPodId(), request.getProductId());

        // Validate POD exists
        Pod pod = podRepository.findById(request.getPodId())
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new PodNotFoundException("POD not found: " + request.getPodId()));

        // Validate product exists
        MirrorProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProductId()));

        // Check if QR code already exists for this POD-product combination
        Optional<PodQrCode> existing = qrCodeRepository.findByPodIdAndProductId(request.getPodId(), request.getProductId());
        if (existing.isPresent()) {
            throw new IllegalStateException("QR code already exists for this POD-product combination");
        }

        // Generate short code
        String shortCode = request.getCustomShortCode() != null
                ? validateAndUseCustomShortCode(request.getCustomShortCode())
                : generateUniqueShortCode();

        // Build full URL
        String fullUrl = qrBaseUrl + shortCode;

        // Create QR code entity
        PodQrCode qrCode = PodQrCode.builder()
                .podId(request.getPodId())
                .productId(request.getProductId())
                .shortCode(shortCode)
                .fullUrl(fullUrl)
                .status(QrCodeStatus.ACTIVE)
                .scanCount(0L)
                .expiresAt(request.getExpiresAt())
                .build();

        qrCode = qrCodeRepository.save(qrCode);

        // Generate and upload QR image
        try {
            String imageUrl = generateAndUploadQrImage(shortCode, fullUrl);
            qrCode.setQrImageUrl(imageUrl);
            qrCode = qrCodeRepository.save(qrCode);
        } catch (Exception e) {
            log.error("Failed to generate QR image for code: {}", shortCode, e);
            // Continue without image - can be regenerated later
        }

        log.info("QR code created with short code: {}", shortCode);
        return QrCodeResponse.fromEntity(qrCode);
    }

    /**
     * Create QR codes for multiple products in a POD
     */
    @Transactional
    public List<QrCodeResponse> createQrCodesBatch(QrCodeBatchCreateRequest request) {
        log.info("Creating batch QR codes for POD: {} with {} products",
                request.getPodId(), request.getProductIds().size());

        // Validate POD exists
        Pod pod = podRepository.findById(request.getPodId())
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new PodNotFoundException("POD not found: " + request.getPodId()));

        List<QrCodeResponse> results = new ArrayList<>();

        for (String productId : request.getProductIds()) {
            try {
                // Check if QR code already exists
                Optional<PodQrCode> existing = qrCodeRepository.findByPodIdAndProductId(request.getPodId(), productId);

                if (existing.isPresent()) {
                    if (request.isRegenerateExisting()) {
                        // Deactivate old and create new
                        PodQrCode oldCode = existing.get();
                        oldCode.setStatus(QrCodeStatus.INACTIVE);
                        qrCodeRepository.save(oldCode);
                    } else {
                        log.info("QR code already exists for product: {}, skipping", productId);
                        results.add(QrCodeResponse.fromEntity(existing.get()));
                        continue;
                    }
                }

                QrCodeCreateRequest createRequest = QrCodeCreateRequest.builder()
                        .podId(request.getPodId())
                        .productId(productId)
                        .expiresAt(request.getExpiresAt())
                        .build();

                results.add(createQrCode(createRequest));
            } catch (Exception e) {
                log.error("Failed to create QR code for product: {}", productId, e);
            }
        }

        log.info("Created {} QR codes in batch", results.size());
        return results;
    }

    /**
     * Get QR code by ID
     */
    @Transactional(readOnly = true)
    public QrCodeResponse getQrCode(String qrCodeId) {
        PodQrCode qrCode = qrCodeRepository.findById(qrCodeId)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new QrCodeNotFoundException("QR code not found: " + qrCodeId));
        return QrCodeResponse.fromEntity(qrCode);
    }

    /**
     * Get QR code by short code
     */
    @Transactional(readOnly = true)
    public QrCodeResponse getQrCodeByShortCode(String shortCode) {
        PodQrCode qrCode = qrCodeRepository.findByShortCode(shortCode)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new QrCodeNotFoundException("QR code not found with short code: " + shortCode));
        return QrCodeResponse.fromEntity(qrCode);
    }

    /**
     * Get QR code detail with statistics
     */
    @Transactional(readOnly = true)
    public QrCodeDetailResponse getQrCodeDetail(String qrCodeId) {
        PodQrCode qrCode = qrCodeRepository.findById(qrCodeId)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new QrCodeNotFoundException("QR code not found: " + qrCodeId));

        QrCodeDetailResponse response = QrCodeDetailResponse.fromEntity(qrCode);

        // Calculate scan statistics (simplified - can be enhanced with date-based queries)
        long totalScans = qrCode.getScanCount();
        response.withStatistics(totalScans, totalScans, 0L, 0L, 0L);

        return response;
    }

    /**
     * Get all QR codes for a POD
     */
    @Transactional(readOnly = true)
    public Page<QrCodeResponse> getQrCodesByPod(String podId, Pageable pageable) {
        return qrCodeRepository.findByPodIdAndIsDeletedFalse(podId, pageable)
                .map(QrCodeResponse::fromEntity);
    }

    /**
     * Search QR codes with criteria
     */
    @Transactional(readOnly = true)
    public Page<QrCodeResponse> searchQrCodes(QrCodeSearchCriteria criteria, Pageable pageable) {
        Specification<PodQrCode> spec = buildQrCodeSpecification(criteria);
        return qrCodeRepository.findAll(spec, pageable).map(QrCodeResponse::fromEntity);
    }

    /**
     * Deactivate a QR code
     */
    @Transactional
    public QrCodeResponse deactivateQrCode(String qrCodeId) {
        log.info("Deactivating QR code: {}", qrCodeId);

        PodQrCode qrCode = qrCodeRepository.findById(qrCodeId)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new QrCodeNotFoundException("QR code not found: " + qrCodeId));

        qrCode.setStatus(QrCodeStatus.INACTIVE);
        qrCode = qrCodeRepository.save(qrCode);

        log.info("QR code deactivated: {}", qrCodeId);
        return QrCodeResponse.fromEntity(qrCode);
    }

    /**
     * Reactivate a QR code
     */
    @Transactional
    public QrCodeResponse reactivateQrCode(String qrCodeId) {
        log.info("Reactivating QR code: {}", qrCodeId);

        PodQrCode qrCode = qrCodeRepository.findById(qrCodeId)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new QrCodeNotFoundException("QR code not found: " + qrCodeId));

        qrCode.setStatus(QrCodeStatus.ACTIVE);
        qrCode = qrCodeRepository.save(qrCode);

        log.info("QR code reactivated: {}", qrCodeId);
        return QrCodeResponse.fromEntity(qrCode);
    }

    /**
     * Delete a QR code (soft delete)
     */
    @Transactional
    public void deleteQrCode(String qrCodeId) {
        log.info("Deleting QR code: {}", qrCodeId);

        PodQrCode qrCode = qrCodeRepository.findById(qrCodeId)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new QrCodeNotFoundException("QR code not found: " + qrCodeId));

        qrCode.setIsDeleted(true);
        qrCode.setStatus(QrCodeStatus.INACTIVE);
        qrCodeRepository.save(qrCode);

        log.info("QR code deleted: {}", qrCodeId);
    }

    /**
     * Regenerate QR image for a code
     */
    @Transactional
    public QrCodeResponse regenerateQrImage(String qrCodeId) {
        log.info("Regenerating QR image for code: {}", qrCodeId);

        PodQrCode qrCode = qrCodeRepository.findById(qrCodeId)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new QrCodeNotFoundException("QR code not found: " + qrCodeId));

        try {
            String imageUrl = generateAndUploadQrImage(qrCode.getShortCode(), qrCode.getFullUrl());
            qrCode.setQrImageUrl(imageUrl);
            qrCode = qrCodeRepository.save(qrCode);
        } catch (Exception e) {
            log.error("Failed to regenerate QR image", e);
            throw new RuntimeException("Failed to regenerate QR image", e);
        }

        return QrCodeResponse.fromEntity(qrCode);
    }

    /**
     * Get QR code entity by short code (for internal use by scan service)
     */
    @Transactional(readOnly = true)
    public Optional<PodQrCode> findByShortCode(String shortCode) {
        return qrCodeRepository.findByShortCode(shortCode)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()));
    }

    /**
     * Expire old QR codes (scheduled job)
     */
    @Transactional
    public int expireOldQrCodes() {
        List<PodQrCode> expired = qrCodeRepository.findExpiredQrCodes(Instant.now());
        for (PodQrCode qrCode : expired) {
            qrCode.setStatus(QrCodeStatus.EXPIRED);
        }
        qrCodeRepository.saveAll(expired);
        log.info("Expired {} QR codes", expired.size());
        return expired.size();
    }

    // ========== Private Helper Methods ==========

    private String generateUniqueShortCode() {
        String shortCode;
        int attempts = 0;
        do {
            shortCode = generateShortCode();
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException("Failed to generate unique short code after 100 attempts");
            }
        } while (qrCodeRepository.existsByShortCode(shortCode));
        return shortCode;
    }

    private String generateShortCode() {
        StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            sb.append(SHORT_CODE_CHARS.charAt(RANDOM.nextInt(SHORT_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String validateAndUseCustomShortCode(String customShortCode) {
        if (customShortCode.length() < 4 || customShortCode.length() > 20) {
            throw new IllegalArgumentException("Short code must be between 4 and 20 characters");
        }
        if (!customShortCode.matches("^[A-Za-z0-9-_]+$")) {
            throw new IllegalArgumentException("Short code can only contain letters, numbers, hyphens, and underscores");
        }
        if (qrCodeRepository.existsByShortCode(customShortCode)) {
            throw new IllegalArgumentException("Short code already exists: " + customShortCode);
        }
        return customShortCode.toUpperCase();
    }

    private String generateAndUploadQrImage(String shortCode, String url) throws WriterException, IOException {
        // Generate QR code image
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);

        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, qrImageSize, qrImageSize, hints);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

        // Convert to PNG bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();

        // Upload to Cloudflare R2
        String key = QR_FOLDER + "/" + shortCode + ".png";
        FileUploadResponse uploadResponse = r2Service.uploadFile(
                key,
                new ByteArrayInputStream(imageBytes),
                imageBytes.length,
                "image/png"
        );

        return uploadResponse.getPublicUrl();
    }

    private Specification<PodQrCode> buildQrCodeSpecification(QrCodeSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude deleted
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
                String keyword = "%" + criteria.getKeyword().toLowerCase() + "%";
                Predicate keywordPredicate = cb.or(
                        cb.like(cb.lower(root.get("shortCode")), keyword)
                );
                predicates.add(keywordPredicate);
            }

            if (criteria.getPodId() != null && !criteria.getPodId().isBlank()) {
                predicates.add(cb.equal(root.get("podId"), criteria.getPodId()));
            }

            if (criteria.getProductId() != null && !criteria.getProductId().isBlank()) {
                predicates.add(cb.equal(root.get("productId"), criteria.getProductId()));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getCreatedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAfter()));
            }

            if (criteria.getCreatedBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedBefore()));
            }

            if (criteria.getMinScanCount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("scanCount"), criteria.getMinScanCount()));
            }

            if (criteria.getMaxScanCount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("scanCount"), criteria.getMaxScanCount()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
