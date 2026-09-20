package com.pipeline.crm.security;

import com.pipeline.crm.common.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force guard for /auth/login. Deliberately simple (no Redis, no external
 * dependency): this app targets at most ~10 concurrent users (see DEVELOPMENT_BACKLOG.md), so a
 * per-instance map is sufficient. Two independent limits apply per attempt:
 * - per client IP (catches a single bot/script hammering any account),
 * - per username (catches distributed/credential-stuffing attempts against one account).
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 8;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final Duration LOCKOUT = Duration.ofMinutes(10);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private record Bucket(int count, Instant windowStart, Instant lockedUntil) {
        static final Bucket EMPTY = new Bucket(0, Instant.EPOCH, Instant.EPOCH);
    }

    public void checkAllowed(String ip, String username) {
        checkKey("ip:" + ip);
        checkKey("user:" + username.toLowerCase());
    }

    public void recordFailure(String ip, String username) {
        failKey("ip:" + ip);
        failKey("user:" + username.toLowerCase());
    }

    public void recordSuccess(String ip, String username) {
        buckets.remove("ip:" + ip);
        buckets.remove("user:" + username.toLowerCase());
    }

    private void checkKey(String key) {
        Bucket b = buckets.getOrDefault(key, Bucket.EMPTY);
        if (Instant.now().isBefore(b.lockedUntil())) {
            throw new TooManyRequestsException("Too many failed login attempts. Try again later.");
        }
    }

    private void failKey(String key) {
        buckets.compute(key, (k, existing) -> {
            Instant now = Instant.now();
            Bucket b = existing == null ? Bucket.EMPTY : existing;
            boolean windowExpired = Duration.between(b.windowStart(), now).compareTo(WINDOW) > 0;
            int count = (windowExpired ? 0 : b.count()) + 1;
            Instant windowStart = windowExpired ? now : b.windowStart();
            Instant lockedUntil = count >= MAX_ATTEMPTS ? now.plus(LOCKOUT) : b.lockedUntil();
            return new Bucket(count, windowStart, lockedUntil);
        });
    }
}
