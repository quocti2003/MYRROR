package com.mirror.product.controller;

import com.mirror.product.dto.sourcing.SourcingReportResponse;
import com.mirror.product.service.EmailRateLimitService;
import com.mirror.product.service.SourcingReportEmailService;
import com.mirror.product.service.SourcingReportPdfService;
import com.mirror.product.service.SourcingReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Sourcing Report operations
 *
 * Provides endpoints for generating, viewing, and managing sourcing reports
 * that are sent to production partners.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sourcing-reports")
@RequiredArgsConstructor
public class SourcingReportController {

    private final SourcingReportService sourcingReportService;
    private final SourcingReportPdfService sourcingReportPdfService;
    private final SourcingReportEmailService sourcingReportEmailService;
    private final EmailRateLimitService emailRateLimitService;

    /**
     * Generate sourcing reports for all vendors assigned to a production plan
     * GET /api/v1/sourcing-reports/by-plan/{planId}
     *
     * Supports optional pagination with page/size parameters
     */
    @GetMapping("/by-plan/{planId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_ADMIN', 'SUPER_ADMIN', 'PRODUCTION_OPS', 'CSO')")
    public ResponseEntity<?> generateReportsForPlan(
            @PathVariable String planId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "false") boolean paginated,
            @AuthenticationPrincipal UserDetails userDetails) {

        String generatedBy = userDetails != null ? userDetails.getUsername() : "system";
        log.info("Generating sourcing reports for plan: {} by user: {} (paginated={}, page={}, size={})",
                planId, generatedBy, paginated, page, size);

        if (paginated) {
            Pageable pageable = PageRequest.of(page, Math.min(size, 100)); // Cap at 100 per page
            Page<SourcingReportResponse> reportPage = sourcingReportService.generateReportsForPlanPaginated(planId, generatedBy, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", reportPage.getContent());
            response.put("currentPage", reportPage.getNumber());
            response.put("totalItems", reportPage.getTotalElements());
            response.put("totalPages", reportPage.getTotalPages());
            response.put("pageSize", reportPage.getSize());
            return ResponseEntity.ok(response);
        } else {
            List<SourcingReportResponse> reports = sourcingReportService.generateReportsForPlan(planId, generatedBy);
            return ResponseEntity.ok(reports);
        }
    }

    /**
     * Generate sourcing report for a specific vendor within a production plan
     * GET /api/v1/sourcing-reports/by-plan/{planId}/vendor/{vendorId}
     */
    @GetMapping("/by-plan/{planId}/vendor/{vendorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_ADMIN', 'SUPER_ADMIN', 'PRODUCTION_OPS', 'CSO')")
    public ResponseEntity<SourcingReportResponse> generateReportForVendor(
            @PathVariable String planId,
            @PathVariable String vendorId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String generatedBy = userDetails != null ? userDetails.getUsername() : "system";
        log.info("Generating sourcing report for plan: {}, vendor: {} by user: {}", planId, vendorId, generatedBy);

        SourcingReportResponse report = sourcingReportService.generateReportForVendor(planId, vendorId, generatedBy);
        return ResponseEntity.ok(report);
    }

    /**
     * Export sourcing report as PDF for a specific vendor
     * GET /api/v1/sourcing-reports/by-plan/{planId}/vendor/{vendorId}/pdf
     *
     * Returns the PDF as a binary attachment
     */
    @GetMapping("/by-plan/{planId}/vendor/{vendorId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_ADMIN', 'SUPER_ADMIN', 'PRODUCTION_OPS', 'CSO')")
    public ResponseEntity<byte[]> exportReportPdf(
            @PathVariable String planId,
            @PathVariable String vendorId,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        String generatedBy = userDetails != null ? userDetails.getUsername() : "system";
        log.info("Exporting PDF sourcing report for plan: {}, vendor: {} by user: {}", planId, vendorId, generatedBy);

        SourcingReportResponse report = sourcingReportService.generateReportForVendor(planId, vendorId, generatedBy);
        byte[] pdfBytes = sourcingReportPdfService.generatePdf(report);

        String filename = String.format("sourcing-report-%s-%s.pdf",
                report.getVendorCode(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    /**
     * Send sourcing report via email to vendor
     * POST /api/v1/sourcing-reports/by-plan/{planId}/vendor/{vendorId}/send
     *
     * Rate limited to 10 requests per hour per user
     */
    @PostMapping("/by-plan/{planId}/vendor/{vendorId}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_ADMIN', 'SUPER_ADMIN', 'PRODUCTION_OPS', 'CSO')")
    public ResponseEntity<Map<String, Object>> sendReportToVendor(
            @PathVariable String planId,
            @PathVariable String vendorId,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        String generatedBy = userDetails != null ? userDetails.getUsername() : "system";

        // Check rate limit
        if (!emailRateLimitService.isAllowedSingleSend(generatedBy)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Rate limit exceeded");
            errorResponse.put("message", "Too many email requests. Please try again later.");
            errorResponse.put("remainingRequests", emailRateLimitService.getRemainingSingleSends(generatedBy));
            errorResponse.put("resetInSeconds", emailRateLimitService.getSecondsUntilResetSingleSend(generatedBy));
            return ResponseEntity.status(429).body(errorResponse);
        }

        log.info("Sending sourcing report email for plan: {}, vendor: {} by user: {}", planId, vendorId, generatedBy);

        SourcingReportResponse report = sourcingReportService.generateReportForVendor(planId, vendorId, generatedBy);
        sourcingReportEmailService.sendReportToVendor(report);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Sourcing report sent successfully to " + report.getVendorEmail());
        response.put("vendorId", vendorId);
        response.put("vendorName", report.getVendorName());
        response.put("vendorEmail", report.getVendorEmail());
        response.put("sentAt", LocalDateTime.now());
        response.put("remainingRequests", emailRateLimitService.getRemainingSingleSends(generatedBy));

        return ResponseEntity.ok(response);
    }

    /**
     * Send sourcing reports to all vendors for a production plan
     * POST /api/v1/sourcing-reports/by-plan/{planId}/send-all
     *
     * Rate limited to 3 bulk requests per hour per user. Emails are queued for async delivery.
     */
    @PostMapping("/by-plan/{planId}/send-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_ADMIN', 'SUPER_ADMIN', 'PRODUCTION_OPS', 'CSO')")
    public ResponseEntity<Map<String, Object>> sendReportsToAllVendors(
            @PathVariable String planId,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        String generatedBy = userDetails != null ? userDetails.getUsername() : "system";

        // Check rate limit (more restrictive for bulk sends)
        if (!emailRateLimitService.isAllowedBulkSend(generatedBy)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Rate limit exceeded");
            errorResponse.put("message", "Too many bulk email requests. Please try again later.");
            errorResponse.put("remainingRequests", emailRateLimitService.getRemainingBulkSends(generatedBy));
            return ResponseEntity.status(429).body(errorResponse);
        }

        log.info("Sending all sourcing reports for plan: {} by user: {}", planId, generatedBy);

        List<SourcingReportResponse> reports = sourcingReportService.generateReportsForPlan(planId, generatedBy);

        int queuedCount = 0;
        int skippedCount = 0;
        List<Map<String, Object>> results = new java.util.ArrayList<>();

        for (SourcingReportResponse report : reports) {
            Map<String, Object> result = new HashMap<>();
            result.put("vendorId", report.getVendorId());
            result.put("vendorName", report.getVendorName());
            result.put("vendorEmail", report.getVendorEmail());

            if (report.getVendorEmail() != null && !report.getVendorEmail().isBlank()) {
                // Send asynchronously - non-blocking
                sourcingReportEmailService.sendReportToVendorAsync(report);
                result.put("status", "queued");
                queuedCount++;
            } else {
                result.put("status", "skipped");
                result.put("reason", "No email address");
                skippedCount++;
            }
            results.add(result);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalVendors", reports.size());
        response.put("queuedCount", queuedCount);
        response.put("skippedCount", skippedCount);
        response.put("results", results);
        response.put("queuedAt", LocalDateTime.now());
        response.put("message", String.format("%d emails queued for delivery, %d skipped (no email)", queuedCount, skippedCount));

        return ResponseEntity.ok(response);
    }
}
