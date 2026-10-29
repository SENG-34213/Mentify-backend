package com.mentify.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

@RequiredArgsConstructor
public class RedisCacheService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RedisFeatureProperties properties;

    public <T> Optional<T> get(String key, Class<T> type) {
        if (!properties.isEnabled()) return Optional.empty();
        try {
            String value = redis.opsForValue().get(key);
            return value == null ? Optional.empty() : Optional.of(objectMapper.readValue(value, type));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public void put(String key, Object value) {
        put(key, value, properties.getCacheDefaultTtl());
    }

    public void put(String key, Object value, Duration ttl) {
        if (!properties.isEnabled() || ttl == null || ttl.isZero() || ttl.isNegative()) return;
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException ignored) {
            // Cache failures must never fail the database-backed request.
        } catch (RuntimeException ignored) {
            // Redis is an optimization and may be unavailable during startup or maintenance.
        }
    }

    public void evict(String... keys) {
        if (!properties.isEnabled() || keys == null || keys.length == 0) return;
        try {
            redis.delete(java.util.List.of(keys));
        } catch (RuntimeException ignored) {
            // Cache invalidation is best effort; the TTL remains the safety net.
        }
    }

    public void evictByPattern(String pattern) {
        if (!properties.isEnabled() || pattern == null || pattern.isBlank()) return;
        try {
            var keys = redis.keys(pattern);
            if (keys != null && !keys.isEmpty()) redis.delete(keys);
        } catch (RuntimeException ignored) {
            // TTL remains the fallback when Redis cannot invalidate immediately.
        }
    }
}
