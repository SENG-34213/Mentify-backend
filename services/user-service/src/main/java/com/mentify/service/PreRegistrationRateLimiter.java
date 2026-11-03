package com.mentify.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limits public pre-registration requests before they reach the database.
 * Redis is used when available so the limit is shared by all user-service
 * instances. The local window is retained as a development fallback.
 */
@Component
public class PreRegistrationRateLimiter {
    private static final String KEY_PREFIX = "mentify:pre-registration:rate-limit:";

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final int ipLimit;
    private final int emailLimit;
    private final Duration window;
    private final ConcurrentHashMap<String, LocalWindow> localWindows = new ConcurrentHashMap<>();

    public PreRegistrationRateLimiter(
            ObjectProvider<StringRedisTemplate> redisProvider,
            @Value("${mentify.pre-registration.rate-limit.ip-limit:5}") int ipLimit,
            @Value("${mentify.pre-registration.rate-limit.email-limit:3}") int emailLimit,
            @Value("${mentify.pre-registration.rate-limit.window-seconds:600}") long windowSeconds) {
        this.redisProvider = redisProvider;
        this.ipLimit = Math.max(1, ipLimit);
        this.emailLimit = Math.max(1, emailLimit);
        this.window = Duration.ofSeconds(Math.max(1, windowSeconds));
    }

    public Decision check(String clientIp, String email) {
        String ipKey = KEY_PREFIX + "ip:" + digest(normalize(clientIp));
        String emailKey = KEY_PREFIX + "email:" + digest(normalize(email));

        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                if (!tryRedis(redis, ipKey, ipLimit) || !tryRedis(redis, emailKey, emailLimit)) {
                    return Decision.BLOCKED;
                }
                return Decision.ALLOWED;
            } catch (RuntimeException ignored) {
                // A local fallback still protects a single running instance if Redis is unavailable.
            }
        }

        if (!tryLocal(ipKey, ipLimit) || !tryLocal(emailKey, emailLimit)) {
            return Decision.BLOCKED;
        }
        return Decision.ALLOWED;
    }

    public long retryAfterSeconds() {
        return window.toSeconds();
    }

    private boolean tryRedis(StringRedisTemplate redis, String key, int limit) {
        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            throw new IllegalStateException("Redis did not return a rate-limit counter");
        }
        if (count == 1L) {
            redis.expire(key, window);
        }
        return count <= limit;
    }

    private boolean tryLocal(String key, int limit) {
        long now = System.currentTimeMillis();
        LocalWindow windowState = localWindows.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt <= now) {
                return new LocalWindow(now + window.toMillis(), 1);
            }
            return new LocalWindow(current.expiresAt, current.count + 1);
        });
        return windowState.count <= limit;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim().toLowerCase();
    }

    private static String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public enum Decision {
        ALLOWED,
        BLOCKED
    }

    private record LocalWindow(long expiresAt, int count) {
    }
}
