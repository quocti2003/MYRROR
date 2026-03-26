package com.mirror.product.service.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.entity.Order;
import com.mirror.product.entity.pod.*;
import com.mirror.product.enums.AttributionStatus;
import com.mirror.product.enums.AttributionType;
import com.mirror.product.exception.pod.PodNotFoundException;
import com.mirror.product.repository.OrderRepository;
import com.mirror.product.repository.pod.*;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PodAttributionService {

    private final PodAttributionRepository attributionRepository;
    private final PodRepository podRepository;
    private final PodQrCodeRepository qrCodeRepository;
    private final PodQrScanRepository scanRepository;
    private final OrderRepository orderRepository;
    private final PodQrScanService scanService;

    @Value("${pod.attribution.window-days:30}")
    private int attributionWindowDays;

    @Value("${pod.attribution.default-type:LAST_TOUCH}")
    private String defaultAttributionType;

    /**
     * Process attribution for an order based on scan history.
     * This is called when an order is placed.
     */
    @Transactional
    public Optional<AttributionResponse> processOrderAttribution(String orderId, HttpServletRequest request) {
        log.info("Processing attribution for order: {}", orderId);

        // Get order
        Order order = orderRepository.findById(orderId)
                .filter(o -> !Boolean.TRUE.equals(o.getIsDeleted()))
                .orElse(null);

        if (order == null) {
            log.warn("Order not found for attribution: {}", orderId);
            return Optional.empty();
        }

        // Check if attribution already exists
        List<PodAttribution> existing = attributionRepository.findByOrderId(orderId);
        if (!existing.isEmpty()) {
            log.info("Attribution already exists for order: {}", orderId);
            return Optional.of(AttributionResponse.fromEntity(existing.get(0)));
        }

        // Try to extract attribution info from cookies
        PodQrScanService.AttributionInfo attrInfo = scanService.extractAttributionInfo(request);
        if (attrInfo != null) {
            return createAttributionFromCookie(order, attrInfo);
        }

        // Try to find scans by user ID
        if (order.getUserId() != null) {
            try {
                Long userId = Long.parseLong(order.getUserId());
                List<PodQrScan> userScans = scanService.getRecentScansByUser(userId);
                if (!userScans.isEmpty()) {
                    return createAttributionFromScans(order, userScans);
                }
            } catch (NumberFormatException e) {
                log.debug("User ID is not a number: {}", order.getUserId());
            }
        }

        log.info("No attribution found for order: {}", orderId);
        return Optional.empty();
    }

    /**
     * Process attribution for an order using session ID (for guest users).
     */
    @Transactional
    public Optional<AttributionResponse> processOrderAttributionBySession(String orderId, String sessionId) {
        log.info("Processing attribution for order: {} with session: {}", orderId, sessionId);

        // Get order
        Order order = orderRepository.findById(orderId)
                .filter(o -> !Boolean.TRUE.equals(o.getIsDeleted()))
                .orElse(null);

        if (order == null) {
            log.warn("Order not found for attribution: {}", orderId);
            return Optional.empty();
        }

        // Check if attribution already exists
        List<PodAttribution> existing = attributionRepository.findByOrderId(orderId);
        if (!existing.isEmpty()) {
            log.info("Attribution already exists for order: {}", orderId);
            return Optional.of(AttributionResponse.fromEntity(existing.get(0)));
        }

        // Find scans by session
        List<PodQrScan> sessionScans = scanService.getRecentScansBySession(sessionId);
        if (!sessionScans.isEmpty()) {
            return createAttributionFromScans(order, sessionScans);
        }

        log.info("No attribution found for order: {} with session: {}", orderId, sessionId);
        return Optional.empty();
    }

    /**
     * Manually create an attribution (admin).
     */
    @Transactional
    public AttributionResponse createAttribution(AttributionCreateRequest request) {
        log.info("Creating manual attribution for order: {} to POD: {}", request.getOrderId(), request.getPodId());

        // Validate order
        Order order = orderRepository.findById(request.getOrderId())
                .filter(o -> !Boolean.TRUE.equals(o.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));

        // Validate POD
        Pod pod = podRepository.findById(request.getPodId())
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new PodNotFoundException("POD not found: " + request.getPodId()));

        // Check for existing attribution
        Optional<PodAttribution> existing = attributionRepository.findByOrderIdAndPodId(
                request.getOrderId(), request.getPodId());
        if (existing.isPresent()) {
            throw new IllegalStateException("Attribution already exists for this order-POD combination");
        }

        // Get order amount
        BigDecimal orderAmount = order.getPaymentOutstanding() != null
                ? order.getPaymentOutstanding()
                : BigDecimal.ZERO;

        // If scan ID provided, get scan details
        String qrCodeId = null;
        Instant firstScanAt = null;
        Instant lastScanAt = null;
        if (request.getScanId() != null) {
            PodQrScan scan = scanRepository.findById(request.getScanId()).orElse(null);
            if (scan != null) {
                qrCodeId = scan.getQrCodeId();
                firstScanAt = scan.getScannedAt();
                lastScanAt = scan.getScannedAt();
            }
        }

        // Create attribution
        PodAttribution attribution = PodAttribution.builder()
                .orderId(request.getOrderId())
                .podId(request.getPodId())
                .partnerId(pod.getPartnerId())
                .qrCodeId(qrCodeId)
                .scanId(request.getScanId())
                .attributionType(request.getAttributionType())
                .attributionWeight(request.getAttributionWeight())
                .orderAmount(orderAmount)
                .attributedAmount(orderAmount.multiply(request.getAttributionWeight()))
                .firstScanAt(firstScanAt)
                .lastScanAt(lastScanAt)
                .orderPlacedAt(order.getCreatedAt())
                .status(AttributionStatus.PENDING)
                .notes(request.getNotes())
                .build();

        PodAttribution saved = attributionRepository.save(attribution);
        log.info("Attribution created: {}", saved.getId());

        return AttributionResponse.fromEntity(saved);
    }

    /**
     * Get attribution by ID.
     */
    @Transactional(readOnly = true)
    public AttributionResponse getAttribution(String attributionId) {
        PodAttribution attribution = attributionRepository.findById(attributionId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Attribution not found: " + attributionId));
        return AttributionResponse.fromEntity(attribution);
    }

    /**
     * Get attributions by order ID.
     */
    @Transactional(readOnly = true)
    public List<AttributionResponse> getAttributionsByOrder(String orderId) {
        return attributionRepository.findByOrderId(orderId).stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .map(AttributionResponse::fromEntity)
                .toList();
    }

    /**
     * Get attributions by partner ID with pagination.
     */
    @Transactional(readOnly = true)
    public Page<AttributionResponse> getAttributionsByPartner(String partnerId, Pageable pageable) {
        return attributionRepository.findByPartnerIdAndIsDeletedFalse(partnerId, pageable)
                .map(AttributionResponse::fromEntity);
    }

    /**
     * Search attributions with criteria.
     */
    @Transactional(readOnly = true)
    public Page<AttributionResponse> searchAttributions(AttributionSearchCriteria criteria, Pageable pageable) {
        Specification<PodAttribution> spec = buildSearchSpecification(criteria);
        return attributionRepository.findAll(spec, pageable).map(AttributionResponse::fromEntity);
    }

    /**
     * Update attribution status.
     */
    @Transactional
    public AttributionResponse updateStatus(String attributionId, AttributionStatusUpdateRequest request) {
        log.info("Updating attribution status: {} to {}", attributionId, request.getStatus());

        PodAttribution attribution = attributionRepository.findById(attributionId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Attribution not found: " + attributionId));

        // Validate status transition
        validateStatusTransition(attribution.getStatus(), request.getStatus());

        attribution.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            attribution.setNotes(request.getNotes());
        }

        attribution = attributionRepository.save(attribution);
        log.info("Attribution status updated: {}", attributionId);

        return AttributionResponse.fromEntity(attribution);
    }

    /**
     * Confirm attribution (shorthand for updating to CONFIRMED).
     */
    @Transactional
    public AttributionResponse confirmAttribution(String attributionId) {
        return updateStatus(attributionId, AttributionStatusUpdateRequest.builder()
                .status(AttributionStatus.CONFIRMED)
                .build());
    }

    /**
     * Cancel attribution.
     */
    @Transactional
    public AttributionResponse cancelAttribution(String attributionId, String reason) {
        return updateStatus(attributionId, AttributionStatusUpdateRequest.builder()
                .status(AttributionStatus.CANCELLED)
                .notes(reason)
                .build());
    }

    /**
     * Delete attribution (soft delete).
     */
    @Transactional
    public void deleteAttribution(String attributionId) {
        log.info("Deleting attribution: {}", attributionId);

        PodAttribution attribution = attributionRepository.findById(attributionId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Attribution not found: " + attributionId));

        // Cannot delete if already in commission
        if (attribution.getCommissionId() != null) {
            throw new IllegalStateException("Cannot delete attribution that is part of a commission");
        }

        attribution.setIsDeleted(true);
        attributionRepository.save(attribution);
        log.info("Attribution deleted: {}", attributionId);
    }

    /**
     * Get attribution statistics for a partner.
     */
    @Transactional(readOnly = true)
    public AttributionStatistics getPartnerStatistics(String partnerId, Instant start, Instant end) {
        log.info("Getting attribution statistics for partner: {} from {} to {}", partnerId, start, end);

        List<PodAttribution> attributions = attributionRepository.findByPartnerIdAndDateRange(partnerId, start, end);

        long totalCount = attributions.size();
        long confirmedCount = attributions.stream()
                .filter(a -> a.getStatus() == AttributionStatus.CONFIRMED)
                .count();
        long pendingCount = attributions.stream()
                .filter(a -> a.getStatus() == AttributionStatus.PENDING)
                .count();
        long cancelledCount = attributions.stream()
                .filter(a -> a.getStatus() == AttributionStatus.CANCELLED)
                .count();

        BigDecimal totalOrderAmount = attributions.stream()
                .map(PodAttribution::getOrderAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAttributedAmount = attributions.stream()
                .filter(a -> a.getStatus() == AttributionStatus.CONFIRMED)
                .map(PodAttribution::getAttributedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgOrderAmount = totalCount > 0
                ? totalOrderAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Double conversionRate = attributionRepository.calculateConversionRateByPartnerId(partnerId);

        // Days to conversion stats
        List<Integer> daysToConversion = attributions.stream()
                .map(PodAttribution::getDaysToConversion)
                .filter(d -> d != null)
                .toList();

        Double avgDays = daysToConversion.isEmpty() ? null
                : daysToConversion.stream().mapToInt(Integer::intValue).average().orElse(0);
        Integer minDays = daysToConversion.isEmpty() ? null
                : daysToConversion.stream().mapToInt(Integer::intValue).min().orElse(0);
        Integer maxDays = daysToConversion.isEmpty() ? null
                : daysToConversion.stream().mapToInt(Integer::intValue).max().orElse(0);

        return AttributionStatistics.builder()
                .partnerId(partnerId)
                .totalAttributions(totalCount)
                .confirmedAttributions(confirmedCount)
                .pendingAttributions(pendingCount)
                .cancelledAttributions(cancelledCount)
                .totalOrderAmount(totalOrderAmount)
                .totalAttributedAmount(totalAttributedAmount)
                .averageOrderAmount(avgOrderAmount)
                .conversionRate(conversionRate)
                .averageDaysToConversion(avgDays)
                .minDaysToConversion(minDays)
                .maxDaysToConversion(maxDays)
                .startDate(start)
                .endDate(end)
                .build();
    }

    /**
     * Get unprocessed attributions for commission calculation.
     */
    @Transactional(readOnly = true)
    public List<PodAttribution> getUnprocessedAttributions(String partnerId) {
        return attributionRepository.findUnprocessedAttributions(partnerId, AttributionStatus.CONFIRMED);
    }

    // ========== Private Helper Methods ==========

    private Optional<AttributionResponse> createAttributionFromCookie(Order order, PodQrScanService.AttributionInfo attrInfo) {
        log.info("Creating attribution from cookie for order: {}", order.getId());

        // Validate POD exists
        Pod pod = podRepository.findById(attrInfo.getPodId())
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElse(null);

        if (pod == null) {
            log.warn("POD not found from attribution cookie: {}", attrInfo.getPodId());
            return Optional.empty();
        }

        // Check attribution window
        Instant windowStart = Instant.now().minus(Duration.ofDays(attributionWindowDays));
        if (attrInfo.getScanTime().isBefore(windowStart)) {
            log.info("Scan is outside attribution window for order: {}", order.getId());
            return Optional.empty();
        }

        // Get order amount
        BigDecimal orderAmount = order.getPaymentOutstanding() != null
                ? order.getPaymentOutstanding()
                : BigDecimal.ZERO;

        // Create attribution
        PodAttribution attribution = PodAttribution.builder()
                .orderId(order.getId())
                .podId(attrInfo.getPodId())
                .partnerId(pod.getPartnerId())
                .attributionType(AttributionType.LAST_TOUCH)
                .attributionWeight(BigDecimal.ONE)
                .orderAmount(orderAmount)
                .attributedAmount(orderAmount)
                .firstScanAt(attrInfo.getScanTime())
                .lastScanAt(attrInfo.getScanTime())
                .orderPlacedAt(order.getCreatedAt())
                .touchCount(1)
                .status(AttributionStatus.PENDING)
                .build();

        attribution = attributionRepository.save(attribution);
        log.info("Attribution created from cookie: {}", attribution.getId());

        return Optional.of(AttributionResponse.fromEntity(attribution));
    }

    private Optional<AttributionResponse> createAttributionFromScans(Order order, List<PodQrScan> scans) {
        log.info("Creating attribution from {} scans for order: {}", scans.size(), order.getId());

        // Sort scans by time
        scans.sort((a, b) -> a.getScannedAt().compareTo(b.getScannedAt()));

        // Determine attribution type
        AttributionType type = AttributionType.valueOf(defaultAttributionType);

        // Get the scan to attribute based on type
        PodQrScan attributedScan = type == AttributionType.FIRST_TOUCH
                ? scans.get(0)
                : scans.get(scans.size() - 1);

        // Get POD
        Pod pod = podRepository.findById(attributedScan.getPodId())
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElse(null);

        if (pod == null) {
            log.warn("POD not found for scan: {}", attributedScan.getId());
            return Optional.empty();
        }

        // Get order amount
        BigDecimal orderAmount = order.getPaymentOutstanding() != null
                ? order.getPaymentOutstanding()
                : BigDecimal.ZERO;

        // Create attribution
        PodAttribution attribution = PodAttribution.builder()
                .orderId(order.getId())
                .podId(attributedScan.getPodId())
                .partnerId(pod.getPartnerId())
                .qrCodeId(attributedScan.getQrCodeId())
                .scanId(attributedScan.getId())
                .attributionType(type)
                .attributionWeight(BigDecimal.ONE)
                .orderAmount(orderAmount)
                .attributedAmount(orderAmount)
                .firstScanAt(scans.get(0).getScannedAt())
                .lastScanAt(scans.get(scans.size() - 1).getScannedAt())
                .orderPlacedAt(order.getCreatedAt())
                .touchCount(scans.size())
                .status(AttributionStatus.PENDING)
                .build();

        attribution = attributionRepository.save(attribution);
        log.info("Attribution created from scans: {}", attribution.getId());

        return Optional.of(AttributionResponse.fromEntity(attribution));
    }

    private void validateStatusTransition(AttributionStatus from, AttributionStatus to) {
        boolean valid = switch (from) {
            case PENDING -> to == AttributionStatus.CONFIRMED || to == AttributionStatus.CANCELLED;
            case CONFIRMED -> to == AttributionStatus.CANCELLED;
            case CANCELLED -> false; // Cannot change from cancelled
        };

        if (!valid) {
            throw new IllegalStateException("Invalid status transition from " + from + " to " + to);
        }
    }

    private Specification<PodAttribution> buildSearchSpecification(AttributionSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude deleted
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (criteria.getOrderId() != null && !criteria.getOrderId().isBlank()) {
                predicates.add(cb.equal(root.get("orderId"), criteria.getOrderId()));
            }

            if (criteria.getPodId() != null && !criteria.getPodId().isBlank()) {
                predicates.add(cb.equal(root.get("podId"), criteria.getPodId()));
            }

            if (criteria.getPartnerId() != null && !criteria.getPartnerId().isBlank()) {
                predicates.add(cb.equal(root.get("partnerId"), criteria.getPartnerId()));
            }

            if (criteria.getQrCodeId() != null && !criteria.getQrCodeId().isBlank()) {
                predicates.add(cb.equal(root.get("qrCodeId"), criteria.getQrCodeId()));
            }

            if (criteria.getAttributionType() != null) {
                predicates.add(cb.equal(root.get("attributionType"), criteria.getAttributionType()));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getOrderPlacedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderPlacedAt"), criteria.getOrderPlacedAfter()));
            }

            if (criteria.getOrderPlacedBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderPlacedAt"), criteria.getOrderPlacedBefore()));
            }

            if (criteria.getMinOrderAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderAmount"), criteria.getMinOrderAmount()));
            }

            if (criteria.getMaxOrderAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderAmount"), criteria.getMaxOrderAmount()));
            }

            if (criteria.getMinDaysToConversion() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("daysToConversion"), criteria.getMinDaysToConversion()));
            }

            if (criteria.getMaxDaysToConversion() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("daysToConversion"), criteria.getMaxDaysToConversion()));
            }

            if (criteria.getHasCommission() != null) {
                if (criteria.getHasCommission()) {
                    predicates.add(cb.isNotNull(root.get("commissionId")));
                } else {
                    predicates.add(cb.isNull(root.get("commissionId")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
