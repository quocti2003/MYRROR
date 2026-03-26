package com.mirror.product.service.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.entity.pod.*;
import com.mirror.product.entity.user.User;
import com.mirror.product.enums.AttributionStatus;
import com.mirror.product.enums.CommissionStatus;
import com.mirror.product.enums.PodStatus;
import com.mirror.product.enums.QrCodeStatus;
import com.mirror.product.exception.pod.PartnerNotFoundException;
import com.mirror.product.repository.pod.*;
import com.mirror.product.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PartnerPortalService {

    private final PodPartnerRepository partnerRepository;
    private final PodRepository podRepository;
    private final PodQrCodeRepository qrCodeRepository;
    private final PodQrScanRepository scanRepository;
    private final PodAttributionRepository attributionRepository;
    private final PodCommissionRepository commissionRepository;
    private final UserRepository userRepository;

    /**
     * Get partner profile by user ID.
     */
    @Transactional(readOnly = true)
    public PartnerProfileResponse getPartnerProfile(Long userId) {
        PodPartner partner = partnerRepository.findByUserId(userId)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found for user: " + userId));
        return PartnerProfileResponse.fromEntity(partner);
    }

    /**
     * Get partner profile by partner ID.
     */
    @Transactional(readOnly = true)
    public PartnerProfileResponse getPartnerProfileById(String partnerId) {
        PodPartner partner = partnerRepository.findActiveById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + partnerId));
        return PartnerProfileResponse.fromEntity(partner);
    }

    /**
     * Get partner profile by username.
     */
    @Transactional(readOnly = true)
    public PartnerProfileResponse getPartnerProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new PartnerNotFoundException("User not found: " + username));
        return getPartnerProfile(user.getId());
    }

    /**
     * Get dashboard data for a partner.
     */
    @Transactional(readOnly = true)
    public PartnerDashboardResponse getDashboard(String partnerId) {
        log.info("Getting dashboard for partner: {}", partnerId);

        PodPartner partner = partnerRepository.findActiveById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + partnerId));

        // Date ranges
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDate endOfLastMonth = startOfMonth.minusDays(1);

        Instant monthStart = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant lastMonthStart = startOfLastMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant lastMonthEnd = endOfLastMonth.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant now = Instant.now();

        // POD stats
        List<Pod> pods = podRepository.findByPartnerId(partnerId);
        int totalPods = pods.size();
        int activePods = (int) pods.stream().filter(p -> p.getStatus() == PodStatus.ACTIVE).count();

        // QR Code stats
        int totalQrCodes = 0;
        int activeQrCodes = 0;
        for (Pod pod : pods) {
            List<PodQrCode> qrCodes = qrCodeRepository.findByPodId(pod.getId());
            totalQrCodes += qrCodes.size();
            activeQrCodes += (int) qrCodes.stream().filter(q -> q.getStatus() == QrCodeStatus.ACTIVE).count();
        }

        // Scan stats
        long totalScans = scanRepository.countByPartnerId(partnerId);
        long scansThisMonth = scanRepository.countByPartnerIdAndDateRange(partnerId, monthStart, now);
        long scansLastMonth = scanRepository.countByPartnerIdAndDateRange(partnerId, lastMonthStart, lastMonthEnd);
        long uniqueScans = scanRepository.countUniqueByPartnerIdAndDateRange(partnerId, lastMonthStart, now);

        double scanGrowth = scansLastMonth > 0
                ? ((double) (scansThisMonth - scansLastMonth) / scansLastMonth) * 100
                : 0;

        // Attribution stats
        long totalAttributions = attributionRepository.countByPartnerId(partnerId);
        long attributionsThisMonth = attributionRepository.countByPartnerIdAndDateRange(partnerId, monthStart, now);
        BigDecimal totalAttributedAmount = attributionRepository.sumAttributedAmountByPartnerId(partnerId);
        BigDecimal attributedThisMonth = attributionRepository.sumAttributedAmountByPartnerIdAndDateRange(
                partnerId, monthStart, now);

        Double conversionRate = attributionRepository.calculateConversionRateByPartnerId(partnerId);

        // Commission stats
        BigDecimal totalEarned = commissionRepository.sumPaidAmountByPartnerId(partnerId);
        BigDecimal pendingCommission = commissionRepository.sumPendingAmountByPartnerId(partnerId);

        List<PodCommission> lastMonthCommissions = commissionRepository.findByPartnerIdAndStatus(
                partnerId, CommissionStatus.PAID);
        BigDecimal lastMonthCommission = lastMonthCommissions.stream()
                .filter(c -> !c.getPeriodEnd().isBefore(startOfLastMonth) && !c.getPeriodEnd().isAfter(endOfLastMonth))
                .map(PodCommission::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Recent scans (last 10)
        List<PartnerDashboardResponse.RecentScan> recentScans = getRecentScans(partnerId, 10);

        // Recent attributions (last 10)
        List<PartnerDashboardResponse.RecentAttribution> recentAttributions = getRecentAttributions(partnerId, 10);

        // Top PODs
        List<PartnerDashboardResponse.TopPod> topPods = getTopPods(partnerId, 5);

        return PartnerDashboardResponse.builder()
                .partnerId(partnerId)
                .businessName(partner.getBusinessName())
                .status(partner.getStatus())
                .tier(partner.getTier())
                .commissionRate(partner.getCommissionRate())
                .totalPods(totalPods)
                .activePods(activePods)
                .totalQrCodes(totalQrCodes)
                .activeQrCodes(activeQrCodes)
                .totalScans(totalScans)
                .uniqueScans(uniqueScans)
                .scansThisMonth(scansThisMonth)
                .scansLastMonth(scansLastMonth)
                .scanGrowthPercent(Math.round(scanGrowth * 100.0) / 100.0)
                .totalAttributions(totalAttributions)
                .attributionsThisMonth(attributionsThisMonth)
                .totalAttributedAmount(totalAttributedAmount != null ? totalAttributedAmount : BigDecimal.ZERO)
                .attributedAmountThisMonth(attributedThisMonth != null ? attributedThisMonth : BigDecimal.ZERO)
                .conversionRate(conversionRate != null ? Math.round(conversionRate * 10000.0) / 100.0 : 0.0)
                .totalEarned(totalEarned != null ? totalEarned : BigDecimal.ZERO)
                .pendingCommission(pendingCommission != null ? pendingCommission : BigDecimal.ZERO)
                .lastMonthCommission(lastMonthCommission)
                .recentScans(recentScans)
                .recentAttributions(recentAttributions)
                .topPods(topPods)
                .build();
    }

    /**
     * Get partner's PODs.
     */
    @Transactional(readOnly = true)
    public Page<PodResponse> getPartnerPods(String partnerId, Pageable pageable) {
        return podRepository.findByPartnerIdAndIsDeletedFalse(partnerId, pageable)
                .map(PodResponse::fromEntity);
    }

    /**
     * Get partner's QR codes.
     */
    @Transactional(readOnly = true)
    public Page<QrCodeResponse> getPartnerQrCodes(String partnerId, Pageable pageable) {
        List<Pod> pods = podRepository.findByPartnerId(partnerId);
        List<String> podIds = pods.stream().map(Pod::getId).toList();

        // This is simplified - in production, you'd want a more efficient query
        List<PodQrCode> allQrCodes = new ArrayList<>();
        for (String podId : podIds) {
            allQrCodes.addAll(qrCodeRepository.findByPodId(podId));
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allQrCodes.size());
        List<QrCodeResponse> content = allQrCodes.subList(start, end).stream()
                .map(QrCodeResponse::fromEntity)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(content, pageable, allQrCodes.size());
    }

    /**
     * Get partner's scans.
     */
    @Transactional(readOnly = true)
    public Page<ScanRecordResponse> getPartnerScans(String partnerId, Pageable pageable) {
        return scanRepository.findByPartnerIdAndIsDeletedFalse(partnerId, pageable)
                .map(ScanRecordResponse::fromEntity);
    }

    /**
     * Get partner's attributions.
     */
    @Transactional(readOnly = true)
    public Page<AttributionResponse> getPartnerAttributions(String partnerId, Pageable pageable) {
        return attributionRepository.findByPartnerIdAndIsDeletedFalse(partnerId, pageable)
                .map(AttributionResponse::fromEntity);
    }

    /**
     * Get partner's commissions.
     */
    @Transactional(readOnly = true)
    public Page<CommissionResponse> getPartnerCommissions(String partnerId, Pageable pageable) {
        return commissionRepository.findByPartnerIdAndIsDeletedFalse(partnerId, pageable)
                .map(CommissionResponse::fromEntity);
    }

    /**
     * Get partner's scan statistics for a date range.
     */
    @Transactional(readOnly = true)
    public PodQrScanService.ScanStatistics getPartnerScanStats(String partnerId, LocalDate startDate, LocalDate endDate) {
        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        long totalScans = scanRepository.countByPartnerIdAndDateRange(partnerId, start, end);
        long uniqueScans = scanRepository.countUniqueByPartnerIdAndDateRange(partnerId, start, end);

        return PodQrScanService.ScanStatistics.builder()
                .totalScans(totalScans)
                .uniqueScans(uniqueScans)
                .startDate(start)
                .endDate(end)
                .build();
    }

    /**
     * Get partner's attribution statistics for a date range.
     */
    @Transactional(readOnly = true)
    public AttributionStatistics getPartnerAttributionStats(String partnerId, LocalDate startDate, LocalDate endDate) {
        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<PodAttribution> attributions = attributionRepository.findByPartnerIdAndDateRange(partnerId, start, end);

        long total = attributions.size();
        long confirmed = attributions.stream().filter(a -> a.getStatus() == AttributionStatus.CONFIRMED).count();
        long pending = attributions.stream().filter(a -> a.getStatus() == AttributionStatus.PENDING).count();
        long cancelled = attributions.stream().filter(a -> a.getStatus() == AttributionStatus.CANCELLED).count();

        BigDecimal totalOrder = attributions.stream()
                .map(PodAttribution::getOrderAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAttributed = attributions.stream()
                .filter(a -> a.getStatus() == AttributionStatus.CONFIRMED)
                .map(PodAttribution::getAttributedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AttributionStatistics.builder()
                .partnerId(partnerId)
                .totalAttributions(total)
                .confirmedAttributions(confirmed)
                .pendingAttributions(pending)
                .cancelledAttributions(cancelled)
                .totalOrderAmount(totalOrder)
                .totalAttributedAmount(totalAttributed)
                .startDate(start)
                .endDate(end)
                .build();
    }

    // ========== Private Helper Methods ==========

    private List<PartnerDashboardResponse.RecentScan> getRecentScans(String partnerId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("scannedAt").descending());
        return scanRepository.findByPartnerIdAndIsDeletedFalse(partnerId, pageable)
                .map(scan -> PartnerDashboardResponse.RecentScan.builder()
                        .scanId(scan.getId())
                        .podName(scan.getPod() != null ? scan.getPod().getName() : null)
                        .productName(scan.getProduct() != null ? scan.getProduct().getItemName() : null)
                        .city(scan.getCity())
                        .deviceType(scan.getDeviceType() != null ? scan.getDeviceType().name() : null)
                        .scannedAt(scan.getScannedAt())
                        .build())
                .getContent();
    }

    private List<PartnerDashboardResponse.RecentAttribution> getRecentAttributions(String partnerId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("orderPlacedAt").descending());
        return attributionRepository.findByPartnerIdAndIsDeletedFalse(partnerId, pageable)
                .map(attr -> PartnerDashboardResponse.RecentAttribution.builder()
                        .attributionId(attr.getId())
                        .orderId(attr.getOrderId())
                        .podName(attr.getPod() != null ? attr.getPod().getName() : null)
                        .orderAmount(attr.getOrderAmount())
                        .attributedAmount(attr.getAttributedAmount())
                        .orderPlacedAt(attr.getOrderPlacedAt())
                        .build())
                .getContent();
    }

    private List<PartnerDashboardResponse.TopPod> getTopPods(String partnerId, int limit) {
        List<Pod> pods = podRepository.findByPartnerId(partnerId);
        return pods.stream()
                .map(pod -> {
                    long scanCount = scanRepository.countByPodId(pod.getId());
                    long attrCount = attributionRepository.countByPodId(pod.getId());
                    return PartnerDashboardResponse.TopPod.builder()
                            .podId(pod.getId())
                            .podName(pod.getName())
                            .locationName(pod.getLocationName())
                            .scanCount(scanCount)
                            .attributionCount(attrCount)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getScanCount(), a.getScanCount()))
                .limit(limit)
                .toList();
    }
}
