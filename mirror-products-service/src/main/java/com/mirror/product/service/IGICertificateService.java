package com.mirror.product.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.dto.IGIReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Using local ConcurrentHashMap cache instead of Spring cache
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service for fetching diamond certificate data from IGI (International Gemological Institute)
 * API Endpoint: https://api.igi.org/ReportDetail.php?Printno={reportNumber}
 *
 * Note: The IGI API may be protected by Cloudflare. This service attempts direct access
 * which may work from server environments. Results are cached permanently since
 * diamond certificates never change after issuance.
 */
@Service
public class IGICertificateService {

    private static final Logger logger = LoggerFactory.getLogger(IGICertificateService.class);

    private static final String IGI_API_URL = "https://api.igi.org/ReportDetail.php";

    // Pattern to validate IGI report numbers (typically alphanumeric, 9-12 characters)
    private static final Pattern REPORT_NUMBER_PATTERN = Pattern.compile("^[A-Za-z0-9]{6,15}$");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Local cache for permanent storage (diamond reports never change)
    private final ConcurrentHashMap<String, IGIReportResponse> localCache = new ConcurrentHashMap<>();

    public IGICertificateService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetch IGI certificate report by report number
     * Results are cached permanently since diamond certificates don't change
     *
     * @param reportNumber The IGI report/certificate number (e.g., "LG729548868")
     * @return IGIReportResponse with certificate details
     * @throws IllegalArgumentException if report number is invalid
     * @throws RuntimeException if API call fails
     */
    public IGIReportResponse getReport(String reportNumber) {
        // Validate input
        if (reportNumber == null || reportNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Report number cannot be empty");
        }

        String cleanReportNumber = reportNumber.trim().toUpperCase();

        // Validate format
        if (!REPORT_NUMBER_PATTERN.matcher(cleanReportNumber).matches()) {
            throw new IllegalArgumentException("Invalid report number format: " + reportNumber);
        }

        // Check local cache first
        IGIReportResponse cached = localCache.get(cleanReportNumber);
        if (cached != null) {
            logger.info("Returning cached IGI report for: {}", cleanReportNumber);
            cached.setFromCache(true);
            return cached;
        }

        logger.info("Fetching IGI report from API for: {}", cleanReportNumber);

        try {
            // Build request with browser-like headers to avoid blocking
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept-Language", "en-US,en;q=0.9");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "https://www.igi.org/");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = IGI_API_URL + "?Printno=" + cleanReportNumber;

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String body = response.getBody();

                // Check if we got Cloudflare challenge page
                if (body.contains("Just a moment") || body.contains("cloudflare") || body.contains("cf_chl")) {
                    logger.warn("IGI API returned Cloudflare challenge for report: {}", cleanReportNumber);
                    throw new RuntimeException("IGI API is protected by Cloudflare. Direct access blocked.");
                }

                // Parse JSON array response
                List<IGIReportResponse> reports = objectMapper.readValue(
                    body,
                    new TypeReference<List<IGIReportResponse>>() {}
                );

                if (reports == null || reports.isEmpty()) {
                    throw new RuntimeException("No report found for number: " + cleanReportNumber);
                }

                IGIReportResponse report = reports.get(0);
                report.setFetchedAt(LocalDateTime.now());
                report.setSource("api.igi.org");
                report.setFromCache(false);

                // Store in local cache permanently
                localCache.put(cleanReportNumber, report);

                logger.info("Successfully fetched IGI report: {} - {} {} {}ct",
                    cleanReportNumber,
                    report.getShapeAndCut(),
                    report.getColorGrade() + "/" + report.getClarityGrade(),
                    report.getCaratWeight());

                return report;
            } else {
                throw new RuntimeException("IGI API returned status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error fetching IGI report for {}: {}", cleanReportNumber, e.getMessage());
            throw new RuntimeException("Failed to fetch IGI report: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a report exists in cache
     */
    public boolean isReportCached(String reportNumber) {
        if (reportNumber == null) return false;
        return localCache.containsKey(reportNumber.trim().toUpperCase());
    }

    /**
     * Manually add a report to cache (useful for manual data entry fallback)
     */
    public void cacheReport(IGIReportResponse report) {
        if (report != null && report.getReportNumber() != null) {
            report.setFetchedAt(LocalDateTime.now());
            report.setSource("manual_entry");
            report.setFromCache(true);
            localCache.put(report.getReportNumber().toUpperCase(), report);
            logger.info("Manually cached IGI report: {}", report.getReportNumber());
        }
    }

    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        return new CacheStats(localCache.size());
    }

    /**
     * Clear the local cache (admin function)
     */
    public void clearCache() {
        localCache.clear();
        logger.info("IGI report cache cleared");
    }

    /**
     * Validate if a string looks like a valid IGI report number
     */
    public boolean isValidReportNumber(String reportNumber) {
        if (reportNumber == null || reportNumber.trim().isEmpty()) {
            return false;
        }
        return REPORT_NUMBER_PATTERN.matcher(reportNumber.trim()).matches();
    }

    /**
     * Inner class for cache statistics
     */
    public static class CacheStats {
        private final int cachedReports;
        private final LocalDateTime timestamp;

        public CacheStats(int cachedReports) {
            this.cachedReports = cachedReports;
            this.timestamp = LocalDateTime.now();
        }

        public int getCachedReports() {
            return cachedReports;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
