package com.mentify.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class RedisLockService {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redis;
    private final RedisFeatureProperties properties;

    public String tryAcquire(String key) {
        if (!properties.isEnabled()) return null;
        String token = UUID.randomUUID().toString();
        Duration ttl = properties.getLockDefaultTtl();
        Boolean acquired = redis.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    public boolean release(String key, String token) {
        if (!properties.isEnabled() || token == null) return false;
        Long released = redis.execute(RELEASE_SCRIPT, List.of(key), token);
        return Long.valueOf(1L).equals(released);
    }
}
