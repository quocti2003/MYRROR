package com.mirror.product.service.pod;

import com.mirror.product.dto.pod.AdminDashboardResponse;
import com.mirror.product.entity.pod.*;
import com.mirror.product.enums.*;
import com.mirror.product.repository.pod.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for admin dashboard and analytics.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminDashboardService {

    private final PodPartnerRepository partnerRepository;
    private final PodRepository podRepository;
    private final PodQrCodeRepository qrCodeRepository;
    private final PodQrScanRepository scanRepository;
    private final PodAttributionRepository attributionRepository;
    private final PodCommissionRepository commissionRepository;

    /**
     * Get comprehensive admin dashboard data.
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        log.info("Getting admin dashboard data");

        // Date ranges
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDate endOfLastMonth = startOfMonth.minusDays(1);
        LocalDate thirtyDaysAgo = today.minusDays(30);

        Instant todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant weekStart = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant monthStart = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant lastMonthStart = startOfLastMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant lastMonthEnd = endOfLastMonth.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant thirtyDaysAgoStart = thirtyDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant now = Instant.now();

        // Partner stats
        List<PodPartner> allPartners = partnerRepository.findAll();
        long totalPartners = allPartners.stream().filter(p -> !Boolean.TRUE.equals(p.getIsDeleted())).count();
        long activePartners = allPartners.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getStatus() == PartnerStatus.ACTIVE).count();
        long pendingPartners = allPartners.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getStatus() == PartnerStatus.PENDING).count();
        long suspendedPartners = allPartners.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getStatus() == PartnerStatus.SUSPENDED).count();

        // POD stats
        List<Pod> allPods = podRepository.findAll();
        long totalPods = allPods.stream().filter(p -> !Boolean.TRUE.equals(p.getIsDeleted())).count();
        long activePods = allPods.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getStatus() == PodStatus.ACTIVE).count();
        long maintenancePods = allPods.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getStatus() == PodStatus.MAINTENANCE).count();
        long inactivePods = allPods.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getStatus() == PodStatus.INACTIVE).count();

        // QR Code stats
        List<PodQrCode> allQrCodes = qrCodeRepository.findAll();
        long totalQrCodes = allQrCodes.stream().filter(q -> !Boolean.TRUE.equals(q.getIsDeleted())).count();
        long activeQrCodes = allQrCodes.stream()
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()) && q.getStatus() == QrCodeStatus.ACTIVE).count();

        // Scan stats
        long totalScans = scanRepository.countTotal();
        long scansToday = scanRepository.countByDateRange(todayStart, now);
        long scansThisWeek = scanRepository.countByDateRange(weekStart, now);
        long scansThisMonth = scanRepository.countByDateRange(monthStart, now);
        long scansLastMonth = scanRepository.countByDateRange(lastMonthStart, lastMonthEnd);

        double scanGrowth = scansLastMonth > 0
                ? ((double) (scansThisMonth - scansLastMonth) / scansLastMonth) * 100
                : 0;

        // Attribution stats
        List<PodAttribution> allAttributions = attributionRepository.findAll();
        long totalAttributions = allAttributions.stream().filter(a -> !Boolean.TRUE.equals(a.getIsDeleted())).count();
        long confirmedAttributions = allAttributions.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()) && a.getStatus() == AttributionStatus.CONFIRMED).count();
        long pendingAttributions = allAttributions.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()) && a.getStatus() == AttributionStatus.PENDING).count();

        BigDecimal totalAttributedRevenue = allAttributions.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()) && a.getStatus() == AttributionStatus.CONFIRMED)
                .map(PodAttribution::getAttributedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal attributedRevenueThisMonth = allAttributions.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted())
                        && a.getStatus() == AttributionStatus.CONFIRMED
                        && a.getOrderPlacedAt() != null
                        && !a.getOrderPlacedAt().isBefore(monthStart))
                .map(PodAttribution::getAttributedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double overallConversionRate = attributionRepository.calculateOverallConversionRate();

        // Commission stats
        List<PodCommission> allCommissions = commissionRepository.findAll();
        long totalCommissions = allCommissions.stream().filter(c -> !Boolean.TRUE.equals(c.getIsDeleted())).count();
        long pendingCommissions = allCommissions.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()) && c.getStatus() == CommissionStatus.PENDING).count();
        long approvedCommissions = allCommissions.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()) && c.getStatus() == CommissionStatus.APPROVED).count();
        long paidCommissions = allCommissions.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()) && c.getStatus() == CommissionStatus.PAID).count();

        BigDecimal totalCommissionAmount = allCommissions.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(PodCommission::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingCommissionAmount = allCommissions.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()) && c.getStatus() == CommissionStatus.PENDING)
                .map(PodCommission::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidCommissionAmount = allCommissions.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()) && c.getStatus() == CommissionStatus.PAID)
                .map(PodCommission::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Trends (last 30 days)
        List<AdminDashboardResponse.DailyStats> scanTrend = getScanTrend(thirtyDaysAgo, today);
        List<AdminDashboardResponse.DailyStats> attributionTrend = getAttributionTrend(thirtyDaysAgo, today);

        // Top performers
        List<AdminDashboardResponse.TopPartner> topPartners = getTopPartners(10);
        List<AdminDashboardResponse.TopPod> topPods = getTopPods(10);
        List<AdminDashboardResponse.TopCity> topCities = getTopCities(10);

        // Geographic distribution
        Map<String, Long> podsByCity = getPodsByCity();
        Map<String, Long> scansByCity = getScansByCity();

        return AdminDashboardResponse.builder()
                .totalPartners(totalPartners)
                .activePartners(activePartners)
                .pendingPartners(pendingPartners)
                .suspendedPartners(suspendedPartners)
                .totalPods(totalPods)
                .activePods(activePods)
                .maintenancePods(maintenancePods)
                .inactivePods(inactivePods)
                .totalQrCodes(totalQrCodes)
                .activeQrCodes(activeQrCodes)
                .totalScans(totalScans)
                .scansToday(scansToday)
                .scansThisWeek(scansThisWeek)
                .scansThisMonth(scansThisMonth)
                .scanGrowthPercent(Math.round(scanGrowth * 100.0) / 100.0)
                .totalAttributions(totalAttributions)
                .confirmedAttributions(confirmedAttributions)
                .pendingAttributions(pendingAttributions)
                .totalAttributedRevenue(totalAttributedRevenue)
                .attributedRevenueThisMonth(attributedRevenueThisMonth)
                .overallConversionRate(overallConversionRate != null ? Math.round(overallConversionRate * 10000.0) / 100.0 : 0.0)
                .totalCommissions(totalCommissions)
                .pendingCommissions(pendingCommissions)
                .approvedCommissions(approvedCommissions)
                .paidCommissions(paidCommissions)
                .totalCommissionAmount(totalCommissionAmount)
                .pendingCommissionAmount(pendingCommissionAmount)
                .paidCommissionAmount(paidCommissionAmount)
                .scanTrend(scanTrend)
                .attributionTrend(attributionTrend)
                .topPartners(topPartners)
                .topPods(topPods)
                .topCities(topCities)
                .podsByCity(podsByCity)
                .scansByCity(scansByCity)
                .build();
    }

    // ========== Private Helper Methods ==========

    private List<AdminDashboardResponse.DailyStats> getScanTrend(LocalDate startDate, LocalDate endDate) {
        List<AdminDashboardResponse.DailyStats> trend = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            Instant dayStart = current.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant dayEnd = current.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

            long count = scanRepository.countByDateRange(dayStart, dayEnd);

            trend.add(AdminDashboardResponse.DailyStats.builder()
                    .date(current)
                    .count(count)
                    .amount(null)
                    .build());

            current = current.plusDays(1);
        }

        return trend;
    }

    private List<AdminDashboardResponse.DailyStats> getAttributionTrend(LocalDate startDate, LocalDate endDate) {
        List<AdminDashboardResponse.DailyStats> trend = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            Instant dayStart = current.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant dayEnd = current.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

            List<PodAttribution> dayAttributions = attributionRepository.findByDateRange(dayStart, dayEnd);

            long count = dayAttributions.stream().filter(a -> !Boolean.TRUE.equals(a.getIsDeleted())).count();
            BigDecimal amount = dayAttributions.stream()
                    .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()) && a.getStatus() == AttributionStatus.CONFIRMED)
                    .map(PodAttribution::getAttributedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            trend.add(AdminDashboardResponse.DailyStats.builder()
                    .date(current)
                    .count(count)
                    .amount(amount)
                    .build());

            current = current.plusDays(1);
        }

        return trend;
    }

    private List<AdminDashboardResponse.TopPartner> getTopPartners(int limit) {
        List<PodPartner> partners = partnerRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getStatus() == PartnerStatus.ACTIVE)
                .toList();

        return partners.stream()
                .map(partner -> {
                    List<Pod> partnerPods = podRepository.findByPartnerId(partner.getId());
                    int podCount = partnerPods.size();

                    long scanCount = scanRepository.countByPartnerId(partner.getId());
                    long attributionCount = attributionRepository.countByPartnerId(partner.getId());

                    BigDecimal totalRevenue = attributionRepository.sumAttributedAmountByPartnerId(partner.getId());
                    BigDecimal commissionEarned = commissionRepository.sumPaidAmountByPartnerId(partner.getId());

                    return AdminDashboardResponse.TopPartner.builder()
                            .partnerId(partner.getId())
                            .businessName(partner.getBusinessName())
                            .tier(partner.getTier() != null ? partner.getTier().name() : null)
                            .podCount(podCount)
                            .scanCount(scanCount)
                            .attributionCount(attributionCount)
                            .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                            .commissionEarned(commissionEarned != null ? commissionEarned : BigDecimal.ZERO)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getScanCount(), a.getScanCount()))
                .limit(limit)
                .toList();
    }

    private List<AdminDashboardResponse.TopPod> getTopPods(int limit) {
        List<Pod> pods = podRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getStatus() == PodStatus.ACTIVE)
                .toList();

        return pods.stream()
                .map(pod -> {
                    long scanCount = scanRepository.countByPodId(pod.getId());
                    long attributionCount = attributionRepository.countByPodId(pod.getId());

                    BigDecimal revenue = attributionRepository.sumAttributedAmountByPodId(pod.getId());

                    String partnerName = null;
                    if (pod.getPartner() != null) {
                        partnerName = pod.getPartner().getBusinessName();
                    }

                    return AdminDashboardResponse.TopPod.builder()
                            .podId(pod.getId())
                            .podName(pod.getName())
                            .partnerName(partnerName)
                            .city(pod.getCity())
                            .scanCount(scanCount)
                            .attributionCount(attributionCount)
                            .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getScanCount(), a.getScanCount()))
                .limit(limit)
                .toList();
    }

    private List<AdminDashboardResponse.TopCity> getTopCities(int limit) {
        List<Pod> allPods = podRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .toList();

        Map<String, List<Pod>> podsByCity = allPods.stream()
                .filter(p -> p.getCity() != null)
                .collect(Collectors.groupingBy(Pod::getCity));

        return podsByCity.entrySet().stream()
                .map(entry -> {
                    String city = entry.getKey();
                    List<Pod> cityPods = entry.getValue();
                    int podCount = cityPods.size();

                    long scanCount = 0;
                    long attributionCount = 0;
                    for (Pod pod : cityPods) {
                        scanCount += scanRepository.countByPodId(pod.getId());
                        attributionCount += attributionRepository.countByPodId(pod.getId());
                    }

                    return AdminDashboardResponse.TopCity.builder()
                            .city(city)
                            .podCount(podCount)
                            .scanCount(scanCount)
                            .attributionCount(attributionCount)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getScanCount(), a.getScanCount()))
                .limit(limit)
                .toList();
    }

    private Map<String, Long> getPodsByCity() {
        List<Pod> allPods = podRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .toList();

        return allPods.stream()
                .filter(p -> p.getCity() != null)
                .collect(Collectors.groupingBy(Pod::getCity, Collectors.counting()));
    }

    private Map<String, Long> getScansByCity() {
        List<PodQrScan> allScans = scanRepository.findAll().stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .toList();

        return allScans.stream()
                .filter(s -> s.getCity() != null)
                .collect(Collectors.groupingBy(PodQrScan::getCity, Collectors.counting()));
    }
}
