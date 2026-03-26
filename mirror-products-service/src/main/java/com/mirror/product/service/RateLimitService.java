package com.mirror.product.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple in-memory rate limiting service
 * Tracks request counts by IP address with sliding window
 */
@Service
@Slf4j
public class RateLimitService {

    // Map of IP -> RequestTracker
    private final ConcurrentMap<String, RequestTracker> requestTrackers = new ConcurrentHashMap<>();

    @Value("${rate.limit.upload.max-requests:20}")
    private int maxRequests; // Max requests per window

    @Value("${rate.limit.upload.window-seconds:3600}")
    private int windowSeconds; // Time window in seconds (default 1 hour)

    /**
     * Check if the request is allowed and consume one request if allowed
     * @param clientIp The client IP address
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String clientIp) {
        RequestTracker tracker = requestTrackers.computeIfAbsent(
            clientIp,
            k -> new RequestTracker(maxRequests, windowSeconds)
        );

        boolean allowed = tracker.tryConsume();

        if (!allowed) {
            log.warn("Rate limit exceeded for IP: {}", maskIp(clientIp));
        }

        return allowed;
    }

    /**
     * Get remaining requests for an IP
     */
    public int getRemainingRequests(String clientIp) {
        RequestTracker tracker = requestTrackers.get(clientIp);
        if (tracker == null) {
            return maxRequests;
        }
        return tracker.getRemainingRequests();
    }

    /**
     * Get seconds until rate limit resets
     */
    public long getSecondsUntilReset(String clientIp) {
        RequestTracker tracker = requestTrackers.get(clientIp);
        if (tracker == null) {
            return 0;
        }
        return tracker.getSecondsUntilReset();
    }

    /**
     * Clean up expired trackers (call periodically)
     */
    public void cleanupExpiredTrackers() {
        long now = System.currentTimeMillis();
        requestTrackers.entrySet().removeIf(entry ->
            entry.getValue().isExpired(now, windowSeconds * 2000L)
        );
    }

    /**
     * Mask IP for logging (privacy)
     */
    private String maskIp(String ip) {
        if (ip == null) return "unknown";
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".xxx";
        }
        return ip;
    }

    /**
     * Inner class to track requests for a single IP
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

        public synchronized boolean isExpired(long now, long maxIdleMillis) {
            return (now - windowStart) > maxIdleMillis;
        }
    }
}
