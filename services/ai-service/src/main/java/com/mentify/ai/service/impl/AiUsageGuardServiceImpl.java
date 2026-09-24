package com.mentify.ai.service.impl;

import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.exception.AiQuotaExceededException;
import com.mentify.ai.service.AiUsageGuardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class AiUsageGuardServiceImpl implements AiUsageGuardService {
    private final AiProviderProperties properties;
    private final Map<UsageKey, WindowCounter> minuteCounters = new ConcurrentHashMap<>();
    private final Map<UsageKey, DayCounter> dayCounters = new ConcurrentHashMap<>();

    @Override
    public void assertAllowed(UUID userId, AiFeatureType featureType) {
        enforceMinuteLimit(userId, featureType);
        enforceDailyQuota(userId, featureType);
    }

    private void enforceMinuteLimit(UUID userId, AiFeatureType featureType) {
        int limit = properties.getGuardrails().getMaxRequestsPerMinute();
        if (limit <= 0) {
            return;
        }

        UsageKey key = new UsageKey(userId, featureType);
        long currentMinute = System.currentTimeMillis() / 60_000;
        WindowCounter counter = minuteCounters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.window() != currentMinute) {
                return new WindowCounter(currentMinute, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });

        if (counter.count().get() > limit) {
            throw new AiQuotaExceededException("AI rate limit exceeded");
        }
    }

    private void enforceDailyQuota(UUID userId, AiFeatureType featureType) {
        int limit = properties.getGuardrails().getMaxRequestsPerDay();
        if (limit <= 0) {
            return;
        }

        UsageKey key = new UsageKey(userId, featureType);
        LocalDate today = LocalDate.now();
        DayCounter counter = dayCounters.compute(key, (ignored, existing) -> {
            if (existing == null || !existing.day().equals(today)) {
                return new DayCounter(today, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });

        if (counter.count().get() > limit) {
            throw new AiQuotaExceededException("Daily AI quota exceeded");
        }
    }

    private record UsageKey(UUID userId, AiFeatureType featureType) {
    }

    private record WindowCounter(long window, AtomicInteger count) {
    }

    private record DayCounter(LocalDate day, AtomicInteger count) {
    }
}
