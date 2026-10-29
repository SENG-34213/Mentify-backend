package com.mentify.ai.tool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Small bounded cache for read-only tool results. The cache key contains a
 * SHA-256 fingerprint of the caller token, never the token itself.
 */
public class AiScopedTtlCache {

    private static final int MAX_ENTRIES = 128;

    private final Map<String, Entry> entries = new HashMap<>();

    @SuppressWarnings("unchecked")
    public synchronized <T> T get(
            String toolName,
            String authorizationHeader,
            Duration ttl,
            Supplier<T> loader
    ) {
        String key = toolName + ":" + fingerprint(authorizationHeader);
        long now = System.nanoTime();
        Entry cached = entries.get(key);
        if (cached != null && cached.expiresAtNanos() > now) {
            return (T) cached.value();
        }
        entries.remove(key);

        T loaded = loader.get();
        if (loaded == null) {
            return null;
        }
        if (entries.size() >= MAX_ENTRIES) {
            entries.keySet().stream().findFirst().ifPresent(entries::remove);
        }
        entries.put(key, new Entry(loaded, now + ttl.toNanos()));
        return loaded;
    }

    private String fingerprint(String value) {
        String source = value == null ? "anonymous" : value;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private record Entry(Object value, long expiresAtNanos) {
    }
}
