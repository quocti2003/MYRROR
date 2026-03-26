package com.mirror.product.controller.pod;

import com.mirror.product.dto.pod.AttributionResponse;
import com.mirror.product.service.pod.PodAttributionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Internal controller for POD attribution processing.
 * Called by other services (e.g., order service) when orders are created.
 *
 * Note: This endpoint is under /internal/** which is permitAll in security config.
 * It should be protected at the network level (internal network only).
 * SPRINT_4: Uncomment @RestController when ready to test Sprint 4
 */
@RestController  // ENABLED_SPRINT_4
@RequestMapping("/internal/pod/attribution")
@RequiredArgsConstructor
@Slf4j
public class InternalAttributionController {

    private final PodAttributionService attributionService;

    /**
     * Process attribution for a new order.
     * This is called internally when an order is placed.
     *
     * POST /internal/pod/attribution/process/{orderId}
     */
    @PostMapping("/process/{orderId}")
    public ResponseEntity<AttributionResponse> processOrderAttribution(
            @PathVariable String orderId,
            HttpServletRequest request) {
        log.info("Internal: Processing attribution for order: {}", orderId);

        try {
            Optional<AttributionResponse> response = attributionService.processOrderAttribution(orderId, request);
            return response.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.noContent().build());
        } catch (Exception e) {
            log.error("Error processing attribution for order: {}", orderId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Process attribution for a new order using session ID.
     * Useful when cookies are not available (e.g., server-to-server calls).
     *
     * POST /internal/pod/attribution/process/{orderId}/session
     */
    @PostMapping("/process/{orderId}/session")
    public ResponseEntity<AttributionResponse> processOrderAttributionBySession(
            @PathVariable String orderId,
            @RequestParam String sessionId) {
        log.info("Internal: Processing attribution for order: {} with session: {}", orderId, sessionId);

        try {
            Optional<AttributionResponse> response = attributionService.processOrderAttributionBySession(orderId, sessionId);
            return response.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.noContent().build());
        } catch (Exception e) {
            log.error("Error processing attribution for order: {} with session: {}", orderId, sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check for attribution service.
     *
     * GET /internal/pod/attribution/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("POD Attribution Service is running");
    }
}
