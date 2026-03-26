package com.mirror.product.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate limiting service for email operations
 * More restrictive than general rate limiting to prevent email abuse
 *
 * Limits:
 * - Single email send: 10 requests per hour per user
 * - Bulk email send: 3 requests per hour per user
 */
@Service
@Slf4j
public class EmailRateLimitService {

    // Map of userId -> RequestTracker for single sends
    private final ConcurrentMap<String, RequestTracker> singleSendTrackers = new ConcurrentHashMap<>();

    // Map of userId -> RequestTracker for bulk sends
    private final ConcurrentMap<String, RequestTracker> bulkSendTrackers = new ConcurrentHashMap<>();

    // Single email limits
    private static final int SINGLE_SEND_MAX_REQUESTS = 10;
    private static final int SINGLE_SEND_WINDOW_SECONDS = 3600; // 1 hour

    // Bulk email limits (more restrictive)
    private static final int BULK_SEND_MAX_REQUESTS = 3;
    private static final int BULK_SEND_WINDOW_SECONDS = 3600; // 1 hour

    /**
     * Check if single email send is allowed for the user
     */
    public boolean isAllowedSingleSend(String userId) {
        if (userId == null || userId.isBlank()) {
            userId = "anonymous";
        }

        RequestTracker tracker = singleSendTrackers.computeIfAbsent(
            userId,
            k -> new RequestTracker(SINGLE_SEND_MAX_REQUESTS, SINGLE_SEND_WINDOW_SECONDS)
        );

        boolean allowed = tracker.tryConsume();

        if (!allowed) {
            log.warn("Email rate limit exceeded for user: {} (single send)", userId);
        }

        return allowed;
    }

    /**
     * Check if bulk email send is allowed for the user
     */
    public boolean isAllowedBulkSend(String userId) {
        if (userId == null || userId.isBlank()) {
            userId = "anonymous";
        }

        RequestTracker tracker = bulkSendTrackers.computeIfAbsent(
            userId,
            k -> new RequestTracker(BULK_SEND_MAX_REQUESTS, BULK_SEND_WINDOW_SECONDS)
        );

        boolean allowed = tracker.tryConsume();

        if (!allowed) {
            log.warn("Email rate limit exceeded for user: {} (bulk send)", userId);
        }

        return allowed;
    }

    /**
     * Get remaining single send requests for a user
     */
    public int getRemainingSingleSends(String userId) {
        RequestTracker tracker = singleSendTrackers.get(userId);
        if (tracker == null) {
            return SINGLE_SEND_MAX_REQUESTS;
        }
        return tracker.getRemainingRequests();
    }

    /**
     * Get remaining bulk send requests for a user
     */
    public int getRemainingBulkSends(String userId) {
        RequestTracker tracker = bulkSendTrackers.get(userId);
        if (tracker == null) {
            return BULK_SEND_MAX_REQUESTS;
        }
        return tracker.getRemainingRequests();
    }

    /**
     * Get seconds until rate limit resets for single sends
     */
    public long getSecondsUntilResetSingleSend(String userId) {
        RequestTracker tracker = singleSendTrackers.get(userId);
        if (tracker == null) {
            return 0;
        }
        return tracker.getSecondsUntilReset();
    }

    /**
     * Inner class to track requests for a single user
     */
    private static class RequestTracker {
        private final int maxRequests;
        private final int windowSeconds;
        private int requestCount;
        private long windowStart;

        public RequestTracker(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
            this.requestCount = 0;
            this.windowStart = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            long windowMillis = windowSeconds * 1000L;

            // Reset window if expired
            if (now - windowStart > windowMillis) {
                windowStart = now;
                requestCount = 0;
            }

            // Check if under limit
            if (requestCount < maxRequests) {
                requestCount++;
                return true;
            }

            return false;
        }

        public synchronized int getRemainingRequests() {
            long now = System.currentTimeMillis();
            long windowMillis = windowSeconds * 1000L;

            if (now - windowStart > windowMillis) {
                return maxRequests;
            }

            return Math.max(0, maxRequests - requestCount);
        }

        public synchronized long getSecondsUntilReset() {
            long now = System.currentTimeMillis();
            long windowMillis = windowSeconds * 1000L;
            long elapsed = now - windowStart;

            if (elapsed > windowMillis) {
                return 0;
            }

            return (windowMillis - elapsed) / 1000;
        }
    }
}
