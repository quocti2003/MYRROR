package com.mirror.product.controller.pod;

import com.mirror.product.exception.pod.QrCodeNotFoundException;
import com.mirror.product.service.pod.PodQrScanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;

/**
 * Public controller for processing QR code scans.
 * No authentication required - this is the endpoint that users scan.
 */
@RestController
@RequestMapping("/q")
@RequiredArgsConstructor
@Slf4j
public class QrScanController {

    private final PodQrScanService scanService;

    @Value("${pod.frontend.base-url:https://mirror.vn}")
    private String frontendBaseUrl;

    @Value("${pod.frontend.product-path:/f4g5h6i7-j8k9-0l1m-2n3o-4p5q6r7s8t9u/}")
    private String productPath;

    @Value("${pod.frontend.error-path:/qr-error}")
    private String errorPath;

    /**
     * Process QR code scan and redirect to product page.
     * GET /q/{shortCode}
     *
     * This is the main endpoint that QR codes point to.
     * It processes the scan (tracks it, sets attribution cookies) and redirects to the product.
     */
    @GetMapping("/{shortCode}")
    public void processScan(
            @PathVariable String shortCode,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        log.info("QR scan received for code: {} from IP: {}", shortCode, getClientIp(request));

        try {
            PodQrScanService.ScanResult result = scanService.processScan(shortCode, request, response);

            if (result.isSuccess()) {
                // Redirect to product page with attribution params
                String redirectUrl = frontendBaseUrl + productPath + result.getProductId()
                        + "?ref=pod&pod=" + result.getPodId()
                        + "&qr=" + result.getQrCodeId()
                        + "&t=" + System.currentTimeMillis();

                if (result.getPartnerId() != null) {
                    redirectUrl += "&partner=" + result.getPartnerId();
                }

                log.info("Redirecting scan for {} to: {}", shortCode, redirectUrl);
                response.sendRedirect(redirectUrl);
            } else {
                // Redirect to error page
                log.warn("Scan failed for code: {} - {}", shortCode, result.getMessage());
                response.sendRedirect(frontendBaseUrl + errorPath + "?code=" + shortCode + "&reason=inactive");
            }
        } catch (QrCodeNotFoundException e) {
            log.warn("QR code not found: {}", shortCode);
            response.sendRedirect(frontendBaseUrl + errorPath + "?code=" + shortCode + "&reason=notfound");
        } catch (Exception e) {
            log.error("Error processing scan for code: {}", shortCode, e);
            response.sendRedirect(frontendBaseUrl + errorPath + "?code=" + shortCode + "&reason=error");
        }
    }

    /**
     * Alternative JSON endpoint for API-based scans (e.g., from mobile apps).
     * POST /q/{shortCode}/api
     *
     * Returns JSON instead of redirect.
     */
    @PostMapping("/{shortCode}/api")
    public ResponseEntity<ScanApiResponse> processScanApi(
            @PathVariable String shortCode,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("API scan received for code: {} from IP: {}", shortCode, getClientIp(request));

        try {
            PodQrScanService.ScanResult result = scanService.processScan(shortCode, request, response);

            if (result.isSuccess()) {
                String redirectUrl = frontendBaseUrl + productPath + result.getProductId()
                        + "?ref=pod&pod=" + result.getPodId();

                return ResponseEntity.ok(ScanApiResponse.builder()
                        .success(true)
                        .productId(result.getProductId())
                        .podId(result.getPodId())
                        .partnerId(result.getPartnerId())
                        .redirectUrl(redirectUrl)
                        .isUnique(result.isUnique())
                        .build());
            } else {
                return ResponseEntity.status(HttpStatus.GONE)
                        .body(ScanApiResponse.builder()
                                .success(false)
                                .error("QR code is not active")
                                .build());
            }
        } catch (QrCodeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ScanApiResponse.builder()
                            .success(false)
                            .error("QR code not found")
                            .build());
        } catch (Exception e) {
            log.error("Error processing API scan for code: {}", shortCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ScanApiResponse.builder()
                            .success(false)
                            .error("Internal error processing scan")
                            .build());
        }
    }

    /**
     * Health check endpoint for QR service
     * GET /q/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("QR Scan Service is running");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ScanApiResponse {
        private boolean success;
        private String error;
        private String productId;
        private String podId;
        private String partnerId;
        private String redirectUrl;
        private boolean isUnique;
    }
}
