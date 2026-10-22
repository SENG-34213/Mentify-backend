package com.mentify.redis;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RedisKey {

    public String of(String prefix, String... parts) {
        StringBuilder key = new StringBuilder(prefix == null ? "mentify:v1" : prefix);
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                throw new IllegalArgumentException("Redis key parts must not be blank");
            }
            key.append(':').append(part);
        }
        return key.toString();
    }
}
