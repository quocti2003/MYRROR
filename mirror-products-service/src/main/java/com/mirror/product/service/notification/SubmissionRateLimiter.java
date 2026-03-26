package com.mirror.product.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Component
public class SubmissionRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(SubmissionRateLimiter.class);
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;

    private final Map<String, Deque<Instant>> ipAttempts = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> identityAttempts = new ConcurrentHashMap<>();

    public void validate(String clientIp, Set<String> recipients) {
        Instant now = Instant.now();
        if (clientIp != null && !clientIp.isBlank()) {
            enforce(ipAttempts, clientIp, now, "Too many submissions from this network. Please wait a few minutes and try again.");
        }

        recipients.stream()
                .filter(Objects::nonNull)
                .map(email -> email.toLowerCase(Locale.ROOT).trim())
                .findFirst()
                .ifPresent(email -> enforce(identityAttempts, email, now,
                        "Too many submissions from this email. Please wait a few minutes and try again."));
    }

    private void enforce(Map<String, Deque<Instant>> store, String key, Instant now, String message) {
        Deque<Instant> deque = store.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (deque) {
            prune(deque, now);
            if (deque.size() >= MAX_ATTEMPTS) {
                log.info("Rate limit exceeded for key={}", key);
                throw new ResponseStatusException(TOO_MANY_REQUESTS, message);
            }
            deque.addLast(now);
        }
    }

    private void prune(Deque<Instant> deque, Instant now) {
        Instant cutoff = now.minus(WINDOW);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.removeFirst();
        }
    }
}
