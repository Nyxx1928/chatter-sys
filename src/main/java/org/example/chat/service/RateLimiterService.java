package org.example.chat.service;

import org.example.chat.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-process token-bucket rate limiter.
 *
 * Each (userId, action) pair gets its own bucket. Tokens refill at a fixed
 * rate; if the bucket is empty the request is rejected with a 429-style
 * ValidationException so the caller can surface a friendly message.
 *
 * Default bucket: 5 room-creations per 10 minutes per user.
 */
@Service
public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);

    /** How many room-creation tokens each user starts with (and refills to). */
    private static final int ROOM_CREATE_CAPACITY = 5;

    /** One token is added every this many milliseconds (10 min / 5 = 2 min per token). */
    private static final long ROOM_CREATE_REFILL_INTERVAL_MS = 2 * 60 * 1000L;

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Checks whether the given user is allowed to create a room right now.
     * Throws {@link ValidationException} (HTTP 400) if the rate limit is exceeded.
     *
     * @param userId the ID of the user attempting room creation
     */
    public void checkRoomCreation(Long userId) {
        String key = "room_create:" + userId;
        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(ROOM_CREATE_CAPACITY, ROOM_CREATE_REFILL_INTERVAL_MS));

        if (!bucket.tryConsume()) {
            logger.warn("Rate limit exceeded for room creation by user {}", userId);
            throw new ValidationException(
                    "You are creating rooms too quickly. Please wait a moment before trying again.");
        }
    }

    private static final int FORGOT_PASSWORD_CAPACITY = 3;
    private static final long FORGOT_PASSWORD_REFILL_INTERVAL_MS = 5 * 60 * 1000L;

    /**
     * Checks whether the given email is allowed to request a password reset.
     * 3 requests per email per 15 minutes.
     * Throws {@link org.example.chat.exception.RateLimitExceededException} (429) if exceeded.
     */
    public void checkForgotPassword(String email) {
        String key = "forgot_password:" + email.toLowerCase();
        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(FORGOT_PASSWORD_CAPACITY, FORGOT_PASSWORD_REFILL_INTERVAL_MS));

        if (!bucket.tryConsume()) {
            logger.warn("Rate limit exceeded for forgot-password by email: {}", email);
            throw new org.example.chat.exception.RateLimitExceededException(
                    "Too many password reset requests. Please try again later.");
        }
    }

    // ── Inner class ──────────────────────────────────────────────────────────

    /**
     * Thread-safe token bucket. Tokens refill one-at-a-time based on elapsed
     * time since the last refill check.
     */
    private static final class TokenBucket {

        private final int capacity;
        private final long refillIntervalMs;

        private int tokens;
        private long lastRefillTime;

        TokenBucket(int capacity, long refillIntervalMs) {
            this.capacity = capacity;
            this.refillIntervalMs = refillIntervalMs;
            this.tokens = capacity;
            this.lastRefillTime = Instant.now().toEpochMilli();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = Instant.now().toEpochMilli();
            long elapsed = now - lastRefillTime;
            int tokensToAdd = (int) (elapsed / refillIntervalMs);
            if (tokensToAdd > 0) {
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTime += (long) tokensToAdd * refillIntervalMs;
            }
        }
    }
}
