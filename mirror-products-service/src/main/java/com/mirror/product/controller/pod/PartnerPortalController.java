package com.mirror.product.controller.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.enums.PartnerType;
import com.mirror.product.exception.pod.UnauthorizedPartnerAccessException;
import com.mirror.product.service.pod.PartnerPortalService;
import com.mirror.product.service.pod.PodQrScanService;
import com.mirror.product.service.pod.PodUserAttributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Partner Portal Controller - endpoints for partners to view their own data.
 * Partners access their dashboard, PODs, scans, attributions, and commissions.
 * SPRINT_6: Uncomment @RestController when ready to test Sprint 6
 */
@RestController  // ENABLED_SPRINT_6
@RequestMapping("/api/v1/partner")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('PARTNER')")
public class PartnerPortalController {

    private final PartnerPortalService partnerPortalService;
    private final PodUserAttributionService podUserAttributionService;

    /**
     * Get current partner's profile.
     */
    @GetMapping("/profile")
    public ResponseEntity<PartnerProfileResponse> getMyProfile(Authentication authentication) {
        String username = authentication.getName();
        log.info("Partner user {} requesting their profile", username);

        PartnerProfileResponse response = partnerPortalService.getPartnerProfileByUsername(username);
        return ResponseEntity.ok(response);
    }

    /**
     * Get partner dashboard data.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<PartnerDashboardResponse> getDashboard(Authentication authentication) {
        String username = authentication.getName();
        log.info("Partner user {} requesting dashboard", username);

        PartnerProfileResponse profile = partnerPortalService.getPartnerProfileByUsername(username);
        PartnerDashboardResponse response = partnerPortalService.getDashboard(profile.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Get partner's PODs.
     */
    @GetMapping("/pods")
    public ResponseEntity<Page<PodResponse>> getMyPods(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String username = authentication.getName();
        log.info("Partner user {} requesting their PODs", username);

        PartnerProfileResponse profile = partnerPortalService.getPartnerProfileByUsername(username);
        Page<PodResponse> response = partnerPortalService.getPartnerPods(profile.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get partner's QR codes.
     */
    @GetMapping("/qr-codes")
    public ResponseEntity<Page<QrCodeResponse>> getMyQrCodes(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String username = authentication.getName();
        log.info("Partner user {} requesting their QR codes", username);

        PartnerProfileResponse profile = partnerPortalService.getPartnerProfileByUsername(username);
        Page<QrCodeResponse> response = partnerPortalService.getPartnerQrCodes(profile.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get partner's scans.
     */
    @GetMapping("/scans")
    public ResponseEntity<Page<ScanRecordResponse>> getMyScans(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "scannedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String username = authentication.getName();
        log.info("Partner user {} requesting their scans", username);

        PartnerProfileResponse profile = partnerPortalService.getPartnerProfileByUsername(username);
        Page<ScanRecordResponse> response = partnerPortalService.getPartnerScans(profile.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get partner's attributions. Only for LOCATION partners.
     */
    @GetMapping("/attributions")
    public ResponseEntity<Page<AttributionResponse>> getMyAttributions(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "orderPlacedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String username = authentication.getName();
        log.info("Partner user {} requesting their attributions", username);

        PartnerProfileResponse profile = partnerPortalService.getPartnerProfileByUsername(username);
        requireLocationPartner(profile);
        Page<AttributionResponse> response = partnerPortalService.getPartnerAttributions(profile.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get partner's user attributions (which users are linked to partner's PODs). Only for LOCATION partners.
     */
    @GetMapping("/user-attributions")
    public ResponseEntity<Page<PodUserAttributionResponse>> getMyUserAttributions(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String username = authentication.getName();
        log.info("Partner user {} requesting their user attributions", username);

        PartnerProfileResponse profile = partnerPortalService.getPartnerProfileByUsername(username);
        requireLocationPartner(profile);
        Page<PodUserAttributionResponse> response = podUserAttributionService.getByPartner(profile.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get partner's commissions. Only for LOCATION partners.
     */
    @GetMapping("/commissions")
    public ResponseEntity<Page<CommissionResponse>> getMyCommissions(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "periodEnd", direction = Sort.Direction.DESC) Pageable pageable) {
        String username = authentication.getName();
        log.info("Partner user {} requesting their commissions", username);

        PartnerProfileResponse profile = partnerPortalService.getPartnerProfileByUsername(username);
        requireLocationPartner(profile);
        Page<CommissionResponse> response = partnerPortalService.getPartnerCommissions(profile.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get partner's scan statistics for a date range.
     */
    @GetMapping("/stats/scans")
    public ResponseEntity<PodQrScanService.ScanStatistics> getScanStats(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String username = authentication.getName();
        log.info("Partner user {} requesting scan stats from {} to {}", username, startDate, endDate);

        PartnerProfileResponse profile = partnerPortalService.getPartnerProfileByUsername(username);
        PodQrScanService.ScanStatistics response = partnerPortalService.getPartnerScanStats(
                profile.getId(), startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Get partner's attribution statistics for a date range. Only for LOCATION partners.
     */
    @GetMapping("/stats/attributions")
    public ResponseEntity<AttributionStatistics> getAttributionStats(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String username = authentication.getName();
        log.info("Partner user {} requesting attribution stats from {} to {}", username, startDate, endDate);

        PartnerProfileResponse profile = partnerPortalService.getPartnerProfileByUsername(username);
        requireLocationPartner(profile);
        AttributionStatistics response = partnerPortalService.getPartnerAttributionStats(
                profile.getId(), startDate, endDate);
        return ResponseEntity.ok(response);
    }

    // === HELPER ===

    private void requireLocationPartner(PartnerProfileResponse profile) {
        if (profile.getPartnerType() == PartnerType.PHYGITAL) {
            throw new UnauthorizedPartnerAccessException(
                    "Commissions and attributions are only available for Location partners");
        }
    }
}
