package com.mentify.ai.service.impl;

import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.exception.AiQuotaExceededException;
import com.mentify.ai.service.AiUsageGuardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class AiUsageGuardServiceImpl implements AiUsageGuardService {
    private static final long MINUTE_WINDOW_SECONDS = Duration.ofMinutes(1).toSeconds();
    private static final long HOUR_WINDOW_SECONDS = Duration.ofHours(1).toSeconds();
    private static final long CLEANUP_INTERVAL_REQUESTS = 100;

    private final AiProviderProperties properties;
    private final Clock clock;
    private final Map<UsageKey, WindowCounter> minuteCounters = new ConcurrentHashMap<>();
    private final Map<UsageKey, WindowCounter> hourCounters = new ConcurrentHashMap<>();
    private final Map<UsageKey, DayCounter> dayCounters = new ConcurrentHashMap<>();
    private final AtomicLong requestSequence = new AtomicLong();

    @Override
    public void assertAllowed(UUID userId, AiFeatureType featureType) {
        UsageKey key = new UsageKey(userId, featureType);
        Instant now = Instant.now(clock);

        RateLimitDecision minuteReservation = reserveWindow(
                minuteCounters,
                key,
                properties.getGuardrails().getMaxRequestsPerMinute(),
                MINUTE_WINDOW_SECONDS,
                now,
                "minute"
        );
        if (!minuteReservation.allowed()) {
            throw quotaExceeded(minuteReservation);
        }

        RateLimitDecision hourReservation = null;
        try {
            hourReservation = reserveWindow(
                    hourCounters,
                    key,
                    properties.getGuardrails().getMaxRequestsPerHour(),
                    HOUR_WINDOW_SECONDS,
                    now,
                    "hour"
            );
            if (!hourReservation.allowed()) {
                throw quotaExceeded(hourReservation);
            }

            RateLimitDecision dayReservation = reserveDay(
                    key,
                    properties.getGuardrails().getMaxRequestsPerDay(),
                    now
            );
            if (!dayReservation.allowed()) {
                throw quotaExceeded(dayReservation);
            }

            cleanupStaleCounters(now);
        } catch (AiQuotaExceededException ex) {
            rollbackWindow(minuteCounters, key, minuteReservation);
            rollbackWindow(hourCounters, key, hourReservation);
            throw ex;
        }
    }

    private RateLimitDecision reserveWindow(
            Map<UsageKey, WindowCounter> counters,
            UsageKey key,
            int limit,
            long windowSeconds,
            Instant now,
            String scope
    ) {
        if (limit <= 0) {
            return RateLimitDecision.allowed(scope, limit, 0, 0, 0, false);
        }

        long currentWindow = now.getEpochSecond() / windowSeconds;
        long retryAfterSeconds = Math.max(1, ((currentWindow + 1) * windowSeconds) - now.getEpochSecond());
        AtomicReference<RateLimitDecision> decision = new AtomicReference<>();

        counters.compute(key, (ignored, existing) -> {
            WindowCounter active = existing;
            if (active == null || active.window() != currentWindow) {
                active = new WindowCounter(currentWindow, new AtomicInteger(0));
            }

            int currentCount = active.count().get();
            if (currentCount >= limit) {
                decision.set(RateLimitDecision.denied(scope, limit, currentCount, retryAfterSeconds, currentWindow));
                return active;
            }

            int updatedCount = active.count().incrementAndGet();
            decision.set(RateLimitDecision.allowed(
                    scope,
                    limit,
                    Math.max(0, limit - updatedCount),
                    retryAfterSeconds,
                    currentWindow,
                    true
            ));
            return active;
        });

        return decision.get();
    }

    private RateLimitDecision reserveDay(UsageKey key, int limit, Instant now) {
        if (limit <= 0) {
            return RateLimitDecision.allowed("day", limit, 0, 0, 0, false);
        }

        LocalDate today = LocalDate.now(clock);
        long retryAfterSeconds = Math.max(1, today.plusDays(1).atStartOfDay(clock.getZone()).toEpochSecond() - now.getEpochSecond());
        AtomicReference<RateLimitDecision> decision = new AtomicReference<>();

        dayCounters.compute(key, (ignored, existing) -> {
            DayCounter active = existing;
            if (active == null || !active.day().equals(today)) {
                active = new DayCounter(today, new AtomicInteger(0));
            }

            int currentCount = active.count().get();
            if (currentCount >= limit) {
                decision.set(RateLimitDecision.denied("day", limit, currentCount, retryAfterSeconds, 0));
                return active;
            }

            int updatedCount = active.count().incrementAndGet();
            decision.set(RateLimitDecision.allowed(
                    "day",
                    limit,
                    Math.max(0, limit - updatedCount),
                    retryAfterSeconds,
                    0,
                    false
            ));
            return active;
        });

        return decision.get();
    }

    private void rollbackWindow(Map<UsageKey, WindowCounter> counters, UsageKey key, RateLimitDecision reservation) {
        if (reservation == null || !reservation.reservedWindow()) {
            return;
        }
        counters.computeIfPresent(key, (ignored, existing) -> {
            if (existing.window() != reservation.window()) {
                return existing;
            }
            int updatedCount = existing.count().decrementAndGet();
            return updatedCount <= 0 ? null : existing;
        });
    }

    private AiQuotaExceededException quotaExceeded(RateLimitDecision decision) {
        return new AiQuotaExceededException(
                "AI " + decision.scope() + " rate limit exceeded. Max "
                        + decision.limit() + " request(s) per " + decision.scope()
                        + ". Try again in " + decision.retryAfterSeconds() + " second(s).",
                decision.retryAfterSeconds()
        );
    }

    private void cleanupStaleCounters(Instant now) {
        if (requestSequence.incrementAndGet() % CLEANUP_INTERVAL_REQUESTS != 0) {
            return;
        }

        long currentMinute = now.getEpochSecond() / MINUTE_WINDOW_SECONDS;
        long currentHour = now.getEpochSecond() / HOUR_WINDOW_SECONDS;
        LocalDate today = LocalDate.now(clock);
        minuteCounters.entrySet().removeIf(entry -> entry.getValue().window() < currentMinute);
        hourCounters.entrySet().removeIf(entry -> entry.getValue().window() < currentHour);
        dayCounters.entrySet().removeIf(entry -> entry.getValue().day().isBefore(today));
    }

    private record UsageKey(UUID userId, AiFeatureType featureType) {
    }

    private record WindowCounter(long window, AtomicInteger count) {
    }

    private record DayCounter(LocalDate day, AtomicInteger count) {
    }

    private record RateLimitDecision(
            boolean allowed,
            String scope,
            int limit,
            int remaining,
            long retryAfterSeconds,
            long window,
            boolean reservedWindow
    ) {
        private static RateLimitDecision allowed(
                String scope,
                int limit,
                int remaining,
                long retryAfterSeconds,
                long window,
                boolean reservedWindow
        ) {
            return new RateLimitDecision(true, scope, limit, remaining, retryAfterSeconds, window, reservedWindow);
        }

        private static RateLimitDecision denied(String scope, int limit, int currentCount, long retryAfterSeconds, long window) {
            return new RateLimitDecision(false, scope, limit, 0, retryAfterSeconds, window, false);
        }
    }
}
