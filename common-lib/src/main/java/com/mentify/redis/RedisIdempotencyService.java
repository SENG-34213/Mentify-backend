package com.mentify.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@RequiredArgsConstructor
public class RedisIdempotencyService {

    private final StringRedisTemplate redis;
    private final RedisFeatureProperties properties;

    public boolean claim(String key) {
        if (!properties.isEnabled()) return true;
        Boolean claimed = redis.opsForValue().setIfAbsent(key, "claimed", properties.getIdempotencyTtl());
        return Boolean.TRUE.equals(claimed);
    }

    public void storeResult(String key, String result, Duration ttl) {
        if (!properties.isEnabled()) return;
        redis.opsForValue().set(key, result, ttl == null ? properties.getIdempotencyTtl() : ttl);
    }

    public Optional<String> getResult(String key) {
        if (!properties.isEnabled()) return Optional.empty();
        return Optional.ofNullable(redis.opsForValue().get(key));
    }
}
