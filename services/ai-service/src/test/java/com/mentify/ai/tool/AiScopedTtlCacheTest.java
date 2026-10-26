package com.mentify.ai.tool;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiScopedTtlCacheTest {

    @Test
    void reusesValueForSameCallerWithinTtl() {
        AiScopedTtlCache cache = new AiScopedTtlCache();
        AtomicInteger loads = new AtomicInteger();

        String first = cache.get("tool", "Bearer admin-token", Duration.ofMinutes(1),
                () -> "value-" + loads.incrementAndGet());
        String second = cache.get("tool", "Bearer admin-token", Duration.ofMinutes(1),
                () -> "value-" + loads.incrementAndGet());

        assertEquals("value-1", first);
        assertEquals("value-1", second);
        assertEquals(1, loads.get());
    }

    @Test
    void doesNotShareValueBetweenCallers() {
        AiScopedTtlCache cache = new AiScopedTtlCache();
        AtomicInteger loads = new AtomicInteger();

        String adminValue = cache.get("tool", "Bearer admin-token", Duration.ofMinutes(1),
                () -> "value-" + loads.incrementAndGet());
        String superAdminValue = cache.get("tool", "Bearer super-admin-token", Duration.ofMinutes(1),
                () -> "value-" + loads.incrementAndGet());

        assertEquals("value-1", adminValue);
        assertEquals("value-2", superAdminValue);
        assertEquals(2, loads.get());
    }
}
