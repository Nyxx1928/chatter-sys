package org.example.chat.service;

import org.example.chat.exception.RateLimitExceededException;
import org.example.chat.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
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

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Evicts token buckets that haven't been accessed in the last hour,
     * preventing unbounded memory growth in long-running processes.
     * Runs every 30 minutes.
     */
    @Scheduled(fixedRate = 1_800_000)
    public void evictStaleBuckets() {
        long cutoff = Instant.now().toEpochMilli() - 3_600_000L;
        Iterator<ConcurrentHashMap.Entry<String, TokenBucket>> it = buckets.entrySet().iterator();
        while (it.hasNext()) {
            ConcurrentHashMap.Entry<String, TokenBucket> entry = it.next();
            if (entry.getValue().lastAccessTime < cutoff) {
                it.remove();
            }
        }
    }

    /**
     * Checks whether the given user is allowed to create a room right now.
     * Throws {@link ValidationException} (HTTP 400) if the rate limit is exceeded.
     *
     * @param userId the ID of the user attempting room creation
     */
    public void checkRoomCreation(Long userId) {
        if (!rateLimitEnabled) {
            return;
        }
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
        if (!rateLimitEnabled) {
            return;
        }
        String key = "forgot_password:" + email.toLowerCase();
        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(FORGOT_PASSWORD_CAPACITY, FORGOT_PASSWORD_REFILL_INTERVAL_MS));

        if (!bucket.tryConsume()) {
            logger.warn("Rate limit exceeded for forgot-password by email: {}", email);
            throw new RateLimitExceededException(
                    "Too many password reset requests. Please try again later.");
        }
    }

    // ── Auth endpoint rate limits (ported from JLabs3 patterns) ──────────────

    private static final int REGISTER_CAPACITY = 3;
    private static final long REGISTER_REFILL_INTERVAL_MS = 60 * 60 * 1000L; // 3 per 60 min

    private static final int LOGIN_CAPACITY = 5;
    private static final long LOGIN_REFILL_INTERVAL_MS = 60 * 1000L; // 5 per 1 min

    private static final int VERIFY_EMAIL_CAPACITY = 5;
    private static final long VERIFY_EMAIL_REFILL_INTERVAL_MS = 60 * 1000L; // 5 per 1 min

    private static final int RESEND_VERIFICATION_CAPACITY = 1;
    private static final long RESEND_VERIFICATION_REFILL_INTERVAL_MS = 60 * 1000L; // 1 per 1 min

    private static final int OTP_VERIFY_CAPACITY = 5;
    private static final long OTP_VERIFY_REFILL_INTERVAL_MS = 60 * 1000L; // 5 per 1 min

    /**
     * Rate-limits user registration by IP address.
     * 3 registration attempts per 60 minutes per IP.
     */
    public void checkRegistration(String ipAddress) {
        if (!rateLimitEnabled) {
            return;
        }
        String key = "register:" + ipAddress;
        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(REGISTER_CAPACITY, REGISTER_REFILL_INTERVAL_MS));
        if (!bucket.tryConsume()) {
            logger.warn("Rate limit exceeded for registration from IP: {}", ipAddress);
            throw new RateLimitExceededException(
                    "Too many registration attempts. Please try again later.");
        }
    }

    /**
     * Rate-limits login attempts by username.
     * 5 attempts per 1 minute per username.
     */
    public void checkLogin(String username) {
        if (!rateLimitEnabled) {
            return;
        }
        String key = "login:" + username.toLowerCase();
        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(LOGIN_CAPACITY, LOGIN_REFILL_INTERVAL_MS));
        if (!bucket.tryConsume()) {
            logger.warn("Rate limit exceeded for login: {}", username);
            throw new RateLimitExceededException(
                    "Too many login attempts. Please try again later.");
        }
    }

    /**
     * Rate-limits email verification by IP address.
     * 5 attempts per 1 minute per IP.
     */
    public void checkEmailVerification(String ipAddress) {
        if (!rateLimitEnabled) {
            return;
        }
        String key = "email_verify:" + ipAddress;
        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(VERIFY_EMAIL_CAPACITY, VERIFY_EMAIL_REFILL_INTERVAL_MS));
        if (!bucket.tryConsume()) {
            logger.warn("Rate limit exceeded for email verification from IP: {}", ipAddress);
            throw new RateLimitExceededException(
                    "Too many verification attempts. Please try again later.");
        }
    }

    /**
     * Rate-limits verification email resends by email address.
     * 1 resend per 1 minute per email.
     */
    public void checkResendVerification(String email) {
        if (!rateLimitEnabled) {
            return;
        }
        String key = "resend_verification:" + email.toLowerCase();
        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(RESEND_VERIFICATION_CAPACITY, RESEND_VERIFICATION_REFILL_INTERVAL_MS));
        if (!bucket.tryConsume()) {
            logger.warn("Rate limit exceeded for resend verification: {}", email);
            throw new RateLimitExceededException(
                    "Too many resend attempts. Please try again later.");
        }
    }

    /**
     * Rate-limits OTP verification attempts by IP address.
     * 5 attempts per 1 minute per IP.
     */
    public void checkOtpVerification(String clientIp) {
        if (!rateLimitEnabled) {
            return;
        }
        String key = "otp_verify:" + clientIp;
        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(OTP_VERIFY_CAPACITY, OTP_VERIFY_REFILL_INTERVAL_MS));
        if (!bucket.tryConsume()) {
            logger.warn("Rate limit exceeded for OTP verification from IP: {}", clientIp);
            throw new RateLimitExceededException(
                    "Too many verification attempts. Please try again later.");
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
        volatile long lastAccessTime;

        TokenBucket(int capacity, long refillIntervalMs) {
            this.capacity = capacity;
            this.refillIntervalMs = refillIntervalMs;
            this.tokens = capacity;
            long now = Instant.now().toEpochMilli();
            this.lastRefillTime = now;
            this.lastAccessTime = now;
        }

        synchronized boolean tryConsume() {
            refill();
            lastAccessTime = Instant.now().toEpochMilli();
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
