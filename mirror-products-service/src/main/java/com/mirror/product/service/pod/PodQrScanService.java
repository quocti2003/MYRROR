package com.mirror.product.service.pod;

import com.mirror.product.dto.pod.ScanRecordResponse;
import com.mirror.product.dto.pod.ScanSearchCriteria;
import com.mirror.product.entity.pod.Pod;
import com.mirror.product.entity.pod.PodQrCode;
import com.mirror.product.entity.pod.PodQrScan;
import com.mirror.product.enums.DeviceType;
import com.mirror.product.enums.QrCodeStatus;
import com.mirror.product.exception.pod.QrCodeNotFoundException;
import com.mirror.product.repository.pod.PodQrCodeRepository;
import com.mirror.product.repository.pod.PodQrScanRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PodQrScanService {

    private final PodQrScanRepository scanRepository;
    private final PodQrCodeRepository qrCodeRepository;

    @Value("${pod.attribution.window-days:30}")
    private int attributionWindowDays;

    @Value("${pod.scan.dedup-minutes:5}")
    private int scanDedupMinutes;

    @Value("${pod.cookie.secure:false}")
    private boolean cookieSecure;

    private static final String SESSION_COOKIE_NAME = "pod_session";
    private static final String ATTRIBUTION_COOKIE_NAME = "pod_attr";
    private static final int SESSION_COOKIE_MAX_AGE = 24 * 60 * 60; // 24 hours
    private static final int ATTRIBUTION_COOKIE_MAX_AGE = 30 * 24 * 60 * 60; // 30 days

    /**
     * Process a QR code scan and return the redirect URL
     */
    @Transactional
    public ScanResult processScan(String shortCode, HttpServletRequest request, HttpServletResponse response) {
        log.info("Processing scan for short code: {}", shortCode);

        // Find QR code
        PodQrCode qrCode = qrCodeRepository.findByShortCode(shortCode)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new QrCodeNotFoundException("QR code not found: " + shortCode));

        // Check if QR code is scannable
        if (!qrCode.isScannable()) {
            log.warn("QR code {} is not scannable. Status: {}", shortCode, qrCode.getStatus());
            return ScanResult.builder()
                    .success(false)
                    .message("QR code is not active")
                    .build();
        }

        // Get or create session ID (for cookie tracking)
        getOrCreateSessionId(request, response);

        // Set attribution cookie (scan record will be saved on login)
        Pod pod = qrCode.getPod();
        setAttributionCookie(response, qrCode, pod);

        log.info("QR code scanned: {}, cookie set. Record will be saved on login.", shortCode);

        return ScanResult.builder()
                .success(true)
                .productId(qrCode.getProductId())
                .podId(qrCode.getPodId())
                .partnerId(pod != null ? pod.getPartnerId() : null)
                .qrCodeId(qrCode.getId())
                .isUnique(true)
                .build();
    }

    /**
     * Get scan by ID
     */
    @Transactional(readOnly = true)
    public ScanRecordResponse getScan(String scanId) {
        PodQrScan scan = scanRepository.findById(scanId)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Scan not found: " + scanId));
        return ScanRecordResponse.fromEntity(scan);
    }

    /**
     * Get scans for a QR code
     */
    @Transactional(readOnly = true)
    public Page<ScanRecordResponse> getScansByQrCode(String qrCodeId, Pageable pageable) {
        return scanRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("qrCodeId"), qrCodeId),
                        cb.equal(root.get("isDeleted"), false)
                ),
                pageable
        ).map(ScanRecordResponse::fromEntity);
    }

    /**
     * Get scans for a POD
     */
    @Transactional(readOnly = true)
    public Page<ScanRecordResponse> getScansByPod(String podId, Pageable pageable) {
        return scanRepository.findByPartnerIdAndIsDeletedFalse(podId, pageable)
                .map(ScanRecordResponse::fromEntity);
    }

    /**
     * Get scans for a partner
     */
    @Transactional(readOnly = true)
    public Page<ScanRecordResponse> getScansByPartner(String partnerId, Pageable pageable) {
        return scanRepository.findByPartnerIdAndIsDeletedFalse(partnerId, pageable)
                .map(ScanRecordResponse::fromEntity);
    }

    /**
     * Search scans with criteria
     */
    @Transactional(readOnly = true)
    public Page<ScanRecordResponse> searchScans(ScanSearchCriteria criteria, Pageable pageable) {
        Specification<PodQrScan> spec = buildScanSpecification(criteria);
        return scanRepository.findAll(spec, pageable).map(ScanRecordResponse::fromEntity);
    }

    /**
     * Get recent scans by user ID for attribution
     */
    @Transactional(readOnly = true)
    public List<PodQrScan> getRecentScansByUser(Long userId) {
        Instant since = Instant.now().minus(Duration.ofDays(attributionWindowDays));
        return scanRepository.findRecentScansByUserId(userId, since);
    }

    /**
     * Get recent scans by session ID for attribution
     */
    @Transactional(readOnly = true)
    public List<PodQrScan> getRecentScansBySession(String sessionId) {
        Instant since = Instant.now().minus(Duration.ofDays(attributionWindowDays));
        return scanRepository.findRecentScansBySessionId(sessionId, since);
    }

    /**
     * Get scan statistics for a partner within date range
     */
    @Transactional(readOnly = true)
    public ScanStatistics getPartnerScanStatistics(String partnerId, Instant start, Instant end) {
        long totalScans = scanRepository.countByPartnerIdAndDateRange(partnerId, start, end);
        long uniqueScans = scanRepository.countUniqueByPartnerIdAndDateRange(partnerId, start, end);

        return ScanStatistics.builder()
                .totalScans(totalScans)
                .uniqueScans(uniqueScans)
                .startDate(start)
                .endDate(end)
                .build();
    }

    /**
     * Extract attribution info from cookies or X-Pod-Attribution header
     */
    public AttributionInfo extractAttributionInfo(HttpServletRequest request) {
        String sessionId = null;
        String attributionData = null;

        // 1. Try cookie first
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    sessionId = cookie.getValue();
                } else if (ATTRIBUTION_COOKIE_NAME.equals(cookie.getName())) {
                    attributionData = cookie.getValue();
                }
            }
        }

        // 2. Fallback to X-Pod-Attribution header
        if (attributionData == null) {
            attributionData = request.getHeader("X-Pod-Attribution");
            if (attributionData != null) {
                log.info("Attribution data found in X-Pod-Attribution header");
            }
        }

        if (attributionData == null) {
            return null;
        }

        return parseAttributionData(attributionData, sessionId);
    }

    private AttributionInfo parseAttributionData(String attributionData, String sessionId) {
        // Parse attribution data (format: partnerId|podId|productId|qrCodeId|timestamp)
        String[] parts = attributionData.split("\\|");
        if (parts.length < 5) {
            return null;
        }

        // Validate required fields are not blank
        if (parts[1].isBlank() || parts[3].isBlank()) {
            log.warn("Attribution data missing required podId or qrCodeId");
            return null;
        }

        try {
            Instant scanTime = Instant.ofEpochMilli(Long.parseLong(parts[4]));
            Instant expiryTime = scanTime.plus(Duration.ofDays(attributionWindowDays));

            if (Instant.now().isAfter(expiryTime)) {
                return null; // Attribution expired
            }

            return AttributionInfo.builder()
                    .sessionId(sessionId)
                    .partnerId(parts[0])
                    .podId(parts[1])
                    .productId(parts[2])
                    .qrCodeId(parts[3])
                    .scanTime(scanTime)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse attribution data", e);
            return null;
        }
    }

    // ========== Private Helper Methods ==========

    private String getOrCreateSessionId(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Create new session ID
        String sessionId = UUID.randomUUID().toString();
        Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(SESSION_COOKIE_MAX_AGE);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(cookieSecure);
        response.addCookie(sessionCookie);

        return sessionId;
    }

    private void setAttributionCookie(HttpServletResponse response, PodQrCode qrCode, Pod pod) {
        // Format: partnerId|podId|productId|qrCodeId|timestamp
        String partnerId = pod != null ? pod.getPartnerId() : "";
        String attributionData = String.format("%s|%s|%s|%s|%d",
                partnerId,
                qrCode.getPodId(),
                qrCode.getProductId(),
                qrCode.getId(),
                Instant.now().toEpochMilli()
        );

        Cookie attrCookie = new Cookie(ATTRIBUTION_COOKIE_NAME, attributionData);
        attrCookie.setPath("/");
        attrCookie.setMaxAge(ATTRIBUTION_COOKIE_MAX_AGE);
        attrCookie.setHttpOnly(true);
        attrCookie.setSecure(cookieSecure);
        response.addCookie(attrCookie);
    }

    private Specification<PodQrScan> buildScanSpecification(ScanSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude deleted
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (criteria.getQrCodeId() != null && !criteria.getQrCodeId().isBlank()) {
                predicates.add(cb.equal(root.get("qrCodeId"), criteria.getQrCodeId()));
            }

            if (criteria.getPodId() != null && !criteria.getPodId().isBlank()) {
                predicates.add(cb.equal(root.get("podId"), criteria.getPodId()));
            }

            if (criteria.getProductId() != null && !criteria.getProductId().isBlank()) {
                predicates.add(cb.equal(root.get("productId"), criteria.getProductId()));
            }

            if (criteria.getPartnerId() != null && !criteria.getPartnerId().isBlank()) {
                predicates.add(cb.equal(root.get("partnerId"), criteria.getPartnerId()));
            }

            if (criteria.getSessionId() != null && !criteria.getSessionId().isBlank()) {
                predicates.add(cb.equal(root.get("sessionId"), criteria.getSessionId()));
            }

            if (criteria.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), criteria.getUserId()));
            }

            if (criteria.getDeviceType() != null) {
                predicates.add(cb.equal(root.get("deviceType"), criteria.getDeviceType()));
            }

            if (criteria.getCountry() != null && !criteria.getCountry().isBlank()) {
                predicates.add(cb.equal(root.get("country"), criteria.getCountry()));
            }

            if (criteria.getCity() != null && !criteria.getCity().isBlank()) {
                predicates.add(cb.equal(root.get("city"), criteria.getCity()));
            }

            if (criteria.getScannedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("scannedAt"), criteria.getScannedAfter()));
            }

            if (criteria.getScannedBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("scannedAt"), criteria.getScannedBefore()));
            }

            if (Boolean.TRUE.equals(criteria.getUniqueOnly())) {
                predicates.add(cb.equal(root.get("isUnique"), true));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ========== Inner Classes ==========

    @lombok.Builder
    @lombok.Data
    public static class ScanResult {
        private boolean success;
        private String message;
        private String productId;
        private String podId;
        private String partnerId;
        private String qrCodeId;
        private boolean isUnique;
    }

    @lombok.Builder
    @lombok.Data
    public static class ScanStatistics {
        private long totalScans;
        private long uniqueScans;
        private Instant startDate;
        private Instant endDate;
    }

    @lombok.Builder
    @lombok.Data
    public static class AttributionInfo {
        private String sessionId;
        private String partnerId;
        private String podId;
        private String productId;
        private String qrCodeId;
        private Instant scanTime;
    }
}
