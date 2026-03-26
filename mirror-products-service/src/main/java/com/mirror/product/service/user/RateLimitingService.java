package com.mirror.product.service.user;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimitingService {
    
    private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();
    
    @Value("${rate.limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${rate.limit.registration.capacity:10}")
    private int registrationCapacity;
    
    @Value("${rate.limit.registration.refill:10}")
    private int registrationRefill;
    
    @Value("${rate.limit.registration.duration:60}")
    private int registrationDurationMinutes;
    
    @Value("${rate.limit.authentication.capacity:20}")
    private int authenticationCapacity;
    
    @Value("${rate.limit.authentication.refill:20}")
    private int authenticationRefill;
    
    @Value("${rate.limit.authentication.duration:5}")
    private int authenticationDurationMinutes;
    
    @Value("${rate.limit.general.capacity:200}")
    private int generalCapacity;
    
    @Value("${rate.limit.general.refill:200}")
    private int generalRefill;
    
    @Value("${rate.limit.general.duration:1}")
    private int generalDurationMinutes;
    
    public Bucket createNewBucket(String key, int capacity, int refillTokens, Duration refillDuration) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(refillTokens, refillDuration));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    
    public Bucket getAuthenticationBucket(String key) {
        return cache.computeIfAbsent(key, k -> 
            createNewBucket(k, authenticationCapacity, authenticationRefill, Duration.ofMinutes(authenticationDurationMinutes))
        );
    }
    
    public Bucket getRegistrationBucket(String key) {
        return cache.computeIfAbsent(key, k -> 
            createNewBucket(k, registrationCapacity, registrationRefill, Duration.ofMinutes(registrationDurationMinutes))
        );
    }
    
    public Bucket getGeneralApiBucket(String key) {
        return cache.computeIfAbsent(key, k -> 
            createNewBucket(k, generalCapacity, generalRefill, Duration.ofMinutes(generalDurationMinutes))
        );
    }
    
    public boolean tryConsume(Bucket bucket) {
        if (!rateLimitEnabled) {
            log.debug("Rate limiting is disabled, allowing request");
            return true;
        }
        return bucket.tryConsume(1);
    }
    
    public boolean tryConsume(Bucket bucket, long tokens) {
        if (!rateLimitEnabled) {
            log.debug("Rate limiting is disabled, allowing request");
            return true;
        }
        return bucket.tryConsume(tokens);
    }
    
    public void removeBucket(String key) {
        cache.remove(key);
        log.debug("Removed rate limit bucket for key: {}", key);
    }
    
    public void clearAllBuckets() {
        cache.clear();
        log.info("Cleared all rate limit buckets");
    }
}