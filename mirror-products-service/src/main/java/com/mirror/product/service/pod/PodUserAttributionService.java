package com.mirror.product.service.pod;

import com.mirror.product.dto.pod.PodUserAttributionResponse;
import com.mirror.product.entity.pod.PodQrScan;
import com.mirror.product.entity.pod.PodUserAttribution;
import com.mirror.product.enums.DeviceType;
import com.mirror.product.enums.PodUserAttributionStatus;
import com.mirror.product.repository.pod.PodQrScanRepository;
import com.mirror.product.repository.pod.PodUserAttributionRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PodUserAttributionService {

    private final PodUserAttributionRepository repository;
    private final PodQrScanRepository scanRepository;
    private final PodQrScanService podQrScanService;

    @Value("${pod.attribution.window-days:30}")
    private int attributionWindowDays;

    @Transactional
    public void linkUserFromCookie(Long userId, HttpServletRequest request) {
        // 1. Read pod_attr cookie
        log.info("linkUserFromCookie called for userId: {}", userId);
        PodQrScanService.AttributionInfo info = podQrScanService.extractAttributionInfo(request);
        if (info == null) {
            log.info("No attribution cookie found for userId: {}", userId);
            return;
        }
        log.info("Attribution cookie found for userId: {}, podId: {}, qrCodeId: {}",
                userId, info.getPodId(), info.getQrCodeId());

        String podId = info.getPodId();
        Instant scanTime = info.getScanTime();

        // 1b. Create scan record (dedup by qrCodeId + userId + scannedAt)
        boolean scanExists = scanRepository.existsByQrCodeIdAndUserIdAndScannedAtAndIsDeletedFalse(
                info.getQrCodeId(), userId, scanTime);
        if (!scanExists) {
            // Extract device info from request
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = getClientIp(request);
            DeviceType deviceType = detectDeviceType(userAgent);
            String os = detectOs(userAgent);
            String browser = detectBrowser(userAgent);

            PodQrScan scan = PodQrScan.builder()
                    .podId(podId)
                    .partnerId(info.getPartnerId())
                    .productId(info.getProductId())
                    .qrCodeId(info.getQrCodeId())
                    .sessionId(info.getSessionId())
                    .userId(userId)
                    .scannedAt(scanTime)
                    .isUnique(true)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 500)) : null)
                    .deviceType(deviceType)
                    .os(os)
                    .browser(browser)
                    .build();
            scanRepository.save(scan);
            log.info("Created scan record for user {} at pod {} (device: {}, os: {}, browser: {})",
                    userId, podId, deviceType, os, browser);
        } else {
            log.debug("Scan record already exists for user {} at qrCode {} (dedup)", userId, info.getQrCodeId());
        }

        // 2. Check existing ACTIVE record
        Optional<PodUserAttribution> existing = repository
                .findByUserIdAndPodIdAndStatus(userId, podId, PodUserAttributionStatus.ACTIVE);

        if (existing.isPresent()) {
            PodUserAttribution record = existing.get();
            if (Instant.now().isBefore(record.getExpiresAt())) {
                // Still active - do nothing (keep original, don't reset window)
                log.debug("Active attribution already exists for user {} and pod {}", userId, podId);
                return;
            } else {
                // Expired - mark as EXPIRED
                record.setStatus(PodUserAttributionStatus.EXPIRED);
                repository.save(record);
                log.info("Expired attribution for user {} and pod {}", userId, podId);
            }
        }

        // 3. Create new ACTIVE record
        PodUserAttribution newRecord = PodUserAttribution.builder()
                .userId(userId)
                .podId(podId)
                .partnerId(info.getPartnerId())
                .productId(info.getProductId())
                .firstScanAt(scanTime)
                .expiresAt(scanTime.plus(Duration.ofDays(attributionWindowDays)))
                .status(PodUserAttributionStatus.ACTIVE)
                .build();
        repository.save(newRecord);

        log.info("Created new attribution for user {} -> pod {} (expires: {})",
                userId, podId, newRecord.getExpiresAt());
    }

    /**
     * Get all user attributions (Admin)
     */
    @Transactional(readOnly = true)
    public Page<PodUserAttributionResponse> getAllAttributions(
            String partnerId, String podId, Long userId,
            PodUserAttributionStatus status, Pageable pageable) {

        Specification<PodUserAttribution> spec = buildSpecification(partnerId, podId, userId, status);
        return repository.findAll(spec, pageable).map(PodUserAttributionResponse::fromEntity);
    }

    /**
     * Get attributions by partner (Partner portal)
     */
    @Transactional(readOnly = true)
    public Page<PodUserAttributionResponse> getByPartner(String partnerId, Pageable pageable) {
        return repository.findByPartnerIdAndIsDeletedFalse(partnerId, pageable)
                .map(PodUserAttributionResponse::fromEntity);
    }

    /**
     * Get attribution by ID
     */
    @Transactional(readOnly = true)
    public PodUserAttributionResponse getById(String id) {
        PodUserAttribution entity = repository.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("User attribution not found: " + id));
        return PodUserAttributionResponse.fromEntity(entity);
    }

    private Specification<PodUserAttribution> buildSpecification(
            String partnerId, String podId, Long userId, PodUserAttributionStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (partnerId != null && !partnerId.isBlank()) {
                predicates.add(cb.equal(root.get("partnerId"), partnerId));
            }
            if (podId != null && !podId.isBlank()) {
                predicates.add(cb.equal(root.get("podId"), podId));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private DeviceType detectDeviceType(String userAgent) {
        if (userAgent == null) return DeviceType.UNKNOWN;
        String ua = userAgent.toLowerCase();
        if (ua.contains("tablet") || ua.contains("ipad")) {
            return DeviceType.TABLET;
        }
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return DeviceType.MOBILE;
        }
        return DeviceType.DESKTOP;
    }

    private String detectOs(String userAgent) {
        if (userAgent == null) return null;
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac os") || ua.contains("macintosh")) return "macOS";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("linux")) return "Linux";
        return "Other";
    }

    private String detectBrowser(String userAgent) {
        if (userAgent == null) return null;
        if (userAgent.contains("Edg/")) return "Edge";
        if (userAgent.contains("OPR/") || userAgent.contains("Opera")) return "Opera";
        if (userAgent.contains("Chrome/") && !userAgent.contains("Edg/")) return "Chrome";
        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) return "Safari";
        if (userAgent.contains("Firefox/")) return "Firefox";
        return "Other";
    }
}
