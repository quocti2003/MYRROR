package com.mirror.product.controller;

import com.mirror.product.dto.IGIReportResponse;
import com.mirror.product.service.IGICertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for IGI (International Gemological Institute) Certificate Lookup
 *
 * Provides endpoints to fetch official diamond/gemstone certificate data from IGI
 * using their public API.
 *
 * Base path: /api/igi
 */
@RestController
@RequestMapping("/api/igi")
@CrossOrigin(origins = "*")
public class IGICertificateController {

    private static final Logger logger = LoggerFactory.getLogger(IGICertificateController.class);

    private final IGICertificateService igiCertificateService;

    public IGICertificateController(IGICertificateService igiCertificateService) {
        this.igiCertificateService = igiCertificateService;
    }

    /**
     * Fetch IGI certificate report by report number
     *
     * @param reportNumber The IGI certificate/report number (e.g., "LG729548868")
     * @return IGIReportResponse with full certificate details
     *
     * Example: GET /api/igi/report/LG729548868
     */
    @GetMapping("/report/{reportNumber}")
    public ResponseEntity<?> getReport(@PathVariable String reportNumber) {
        logger.info("IGI report lookup requested for: {}", reportNumber);

        try {
            // Validate report number format
            if (!igiCertificateService.isValidReportNumber(reportNumber)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid report number format",
                    "reportNumber", reportNumber,
                    "hint", "IGI report numbers are typically 9-12 alphanumeric characters"
                ));
            }

            IGIReportResponse report = igiCertificateService.getReport(reportNumber);

            logger.info("IGI report found: {} - {} {}",
                report.getReportNumber(),
                report.getCaratWeight(),
                report.getColorGrade() + "/" + report.getClarityGrade());

            return ResponseEntity.ok(report);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid report number: {} - {}", reportNumber, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "reportNumber", reportNumber
            ));

        } catch (RuntimeException e) {
            logger.error("Error fetching IGI report {}: {}", reportNumber, e.getMessage());

            // Check if it's a Cloudflare block
            if (e.getMessage() != null && e.getMessage().contains("Cloudflare")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "error", "IGI API temporarily unavailable",
                    "reason", "API protected by Cloudflare challenge",
                    "reportNumber", reportNumber,
                    "suggestion", "Try again later or enter certificate data manually"
                ));
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to fetch IGI report",
                "message", e.getMessage(),
                "reportNumber", reportNumber
            ));
        }
    }

    /**
     * Validate if a report number format is correct
     *
     * Example: GET /api/igi/validate/LG729548868
     */
    @GetMapping("/validate/{reportNumber}")
    public ResponseEntity<Map<String, Object>> validateReportNumber(@PathVariable String reportNumber) {
        boolean isValid = igiCertificateService.isValidReportNumber(reportNumber);
        boolean isCached = igiCertificateService.isReportCached(reportNumber);

        Map<String, Object> response = new HashMap<>();
        response.put("reportNumber", reportNumber);
        response.put("validFormat", isValid);
        response.put("cached", isCached);

        if (!isValid) {
            response.put("hint", "IGI report numbers are typically 9-12 alphanumeric characters");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get cache statistics
     *
     * Example: GET /api/igi/cache/stats
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<IGICertificateService.CacheStats> getCacheStats() {
        return ResponseEntity.ok(igiCertificateService.getCacheStats());
    }

    /**
     * Clear the IGI report cache (admin function)
     *
     * Example: DELETE /api/igi/cache
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, String>> clearCache() {
        logger.warn("IGI cache clear requested");
        igiCertificateService.clearCache();
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "IGI report cache cleared"
        ));
    }

    /**
     * Manually cache a report (for manual data entry fallback)
     *
     * Example: POST /api/igi/report
     */
    @PostMapping("/report")
    public ResponseEntity<?> cacheReport(@RequestBody IGIReportResponse report) {
        try {
            if (report.getReportNumber() == null || report.getReportNumber().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Report number is required"
                ));
            }

            igiCertificateService.cacheReport(report);

            logger.info("Manually cached IGI report: {}", report.getReportNumber());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Report cached successfully",
                "reportNumber", report.getReportNumber()
            ));

        } catch (Exception e) {
            logger.error("Error caching IGI report: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to cache report",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Health check for IGI integration
     *
     * Example: GET /api/igi/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "IGI Certificate Service");
        health.put("cacheSize", igiCertificateService.getCacheStats().getCachedReports());
        health.put("apiEndpoint", "https://api.igi.org/ReportDetail.php");

        return ResponseEntity.ok(health);
    }
}
