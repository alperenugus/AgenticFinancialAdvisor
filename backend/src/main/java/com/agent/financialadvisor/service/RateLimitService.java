package com.agent.financialadvisor.service;

import com.agent.financialadvisor.config.RateLimitConfig;
import com.agent.financialadvisor.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting service using Bucket4j (token bucket algorithm).
 * Implements per-session rate limiting to prevent abuse of the advisor API.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    
    private final RateLimitConfig config;
    
    // Per-session buckets for advisor endpoint
    private final Map<String, Bucket> advisorBuckets = new ConcurrentHashMap<>();

    public RateLimitService(RateLimitConfig config) {
        this.config = config;
    }

    /**
     * Check if a request is allowed for the advisor endpoint.
     * Throws RateLimitExceededException if limit is exceeded.
     */
    public void checkAdvisorRateLimit(String sessionId) {
        Bucket bucket = advisorBuckets.computeIfAbsent(sessionId, this::createAdvisorBucket);
        
        if (!bucket.tryConsume(1)) {
            long retryAfter = calculateRetryAfter(bucket);
            int remaining = (int) bucket.getAvailableTokens();
            
            log.warn("Rate limit exceeded for session: {} (advisor endpoint). Retry after: {} seconds", 
                    sessionId, retryAfter);
            
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Please wait before sending another message.",
                    retryAfter,
                    remaining
            );
        }
        
        log.debug("Rate limit check passed for session: {} (advisor endpoint). Remaining tokens: {}", 
                sessionId, bucket.getAvailableTokens());
    }

    /**
     * Create a token bucket for advisor endpoint with configured limits.
     */
    private Bucket createAdvisorBucket(String sessionId) {
        RateLimitConfig.EndpointConfig advisorConfig = config.getAdvisor();
        
        // Use Bandwidth.builder() for the newer API (non-deprecated)
        Bandwidth limit = Bandwidth.builder()
                .capacity(advisorConfig.getCapacity())
                .refillIntervally(advisorConfig.getRefillTokens(), Duration.ofSeconds(advisorConfig.getRefillPeriodSeconds()))
                .build();
        
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Calculate retry-after time in seconds.
     * This estimates when the next token will be available.
     */
    private long calculateRetryAfter(Bucket bucket) {
        // Get the time until the next token refill
        // Bucket4j doesn't directly expose this, so we estimate based on refill period
        RateLimitConfig.EndpointConfig config = this.config.getAdvisor();
        long refillPeriodSeconds = config.getRefillPeriodSeconds();
        long tokensPerRefill = config.getRefillTokens();
        
        // Estimate: if we need tokens, wait at most one refill period
        // In practice, this will be less, but this is a safe estimate
        return Math.max(1, refillPeriodSeconds / tokensPerRefill);
    }

    /**
     * Get remaining tokens for a session (for debugging/monitoring).
     */
    public int getRemainingAdvisorTokens(String sessionId) {
        Bucket bucket = advisorBuckets.get(sessionId);
        if (bucket == null) {
            return config.getAdvisor().getCapacity();
        }
        return (int) bucket.getAvailableTokens();
    }

    /**
     * Clean up buckets for inactive sessions (called by session cleanup).
     */
    public void cleanupSession(String sessionId) {
        advisorBuckets.remove(sessionId);
        log.debug("Cleaned up rate limit buckets for session: {}", sessionId);
    }
}

