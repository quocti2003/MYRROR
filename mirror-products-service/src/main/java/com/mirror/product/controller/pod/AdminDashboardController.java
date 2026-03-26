package com.mirror.product.controller.pod;

import com.mirror.product.dto.pod.AdminDashboardResponse;
import com.mirror.product.service.pod.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for POD Admin Dashboard.
 * Provides system-wide metrics and analytics for administrators.
 * SPRINT_6: Uncomment @RestController when ready to test Sprint 6
 */
@RestController  // ENABLED_SPRINT_6
@RequestMapping("/api/v1/admin/pod-dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('POD_ADMIN_DASHBOARD_VIEW')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * Get comprehensive admin dashboard data.
     * Includes: partner stats, POD stats, QR code stats, scan metrics,
     * attribution stats, commission stats, trends, and top performers.
     *
     * GET /api/v1/admin/pod-dashboard
     */
    @GetMapping
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        log.info("REST request to get admin POD dashboard");
        AdminDashboardResponse response = adminDashboardService.getDashboard();
        return ResponseEntity.ok(response);
    }

    /**
     * Get dashboard summary (lightweight version).
     * Returns only key metrics without trends and detailed breakdowns.
     *
     * GET /api/v1/admin/pod-dashboard/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<AdminDashboardResponse.Summary> getDashboardSummary() {
        log.info("REST request to get admin POD dashboard summary");
        AdminDashboardResponse dashboard = adminDashboardService.getDashboard();

        AdminDashboardResponse.Summary summary = AdminDashboardResponse.Summary.builder()
                .totalPartners(dashboard.getTotalPartners())
                .activePartners(dashboard.getActivePartners())
                .totalPods(dashboard.getTotalPods())
                .activePods(dashboard.getActivePods())
                .totalQrCodes(dashboard.getTotalQrCodes())
                .activeQrCodes(dashboard.getActiveQrCodes())
                .totalScans(dashboard.getTotalScans())
                .scansToday(dashboard.getScansToday())
                .totalAttributions(dashboard.getTotalAttributions())
                .totalAttributedRevenue(dashboard.getTotalAttributedRevenue())
                .overallConversionRate(dashboard.getOverallConversionRate())
                .pendingCommissionAmount(dashboard.getPendingCommissionAmount())
                .paidCommissionAmount(dashboard.getPaidCommissionAmount())
                .build();

        return ResponseEntity.ok(summary);
    }
}
