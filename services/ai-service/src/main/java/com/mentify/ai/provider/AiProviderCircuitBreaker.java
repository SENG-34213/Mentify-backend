package com.mentify.ai.provider;

import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.exception.AiProviderException;
import com.mentify.ai.exception.AiProviderRateLimitException;
import com.mentify.ai.exception.AiProviderTimeoutException;
import com.mentify.ai.exception.AiProviderUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiProviderCircuitBreaker {
    private final AiProviderProperties properties;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, CircuitState> states = new ConcurrentHashMap<>();

    public <T> T execute(String providerName, Supplier<T> operation) {
        CircuitState state = states.computeIfAbsent(providerName.toUpperCase(), key -> new CircuitState());
        if (!state.tryAcquire(providerName, properties.getCircuitBreaker(), clock.instant())) {
            throw new AiProviderUnavailableException("AI provider circuit is open: " + providerName);
        }

        try {
            T result = operation.get();
            state.recordSuccess(providerName);
            return result;
        } catch (RuntimeException ex) {
            if (isCircuitBreakerFailure(ex)) {
                state.recordFailure(providerName, properties.getCircuitBreaker(), clock.instant());
            }
            throw ex;
        }
    }

    private boolean isCircuitBreakerFailure(RuntimeException ex) {
        return ex instanceof AiProviderUnavailableException
                || ex instanceof AiProviderTimeoutException
                || ex instanceof AiProviderRateLimitException;
    }

    private static final class CircuitState {
        private CircuitStatus status = CircuitStatus.CLOSED;
        private int consecutiveFailures;
        private Instant openedAt;

        synchronized boolean tryAcquire(String providerName,
                                        AiProviderProperties.CircuitBreakerConfig config,
                                        Instant now) {
            if (!config.isEnabled() || status == CircuitStatus.CLOSED) {
                return true;
            }

            Duration openDuration = config.getOpenDuration();
            if (status == CircuitStatus.OPEN && openedAt != null && now.isAfter(openedAt.plus(openDuration))) {
                status = CircuitStatus.HALF_OPEN;
                log.info("AI provider circuit half-open: {}", providerName);
                return true;
            }

            return status == CircuitStatus.HALF_OPEN;
        }

        synchronized void recordSuccess(String providerName) {
            if (status != CircuitStatus.CLOSED || consecutiveFailures > 0) {
                log.info("AI provider circuit closed: {}", providerName);
            }
            status = CircuitStatus.CLOSED;
            consecutiveFailures = 0;
            openedAt = null;
        }

        synchronized void recordFailure(String providerName,
                                        AiProviderProperties.CircuitBreakerConfig config,
                                        Instant now) {
            if (!config.isEnabled()) {
                return;
            }

            if (status == CircuitStatus.HALF_OPEN) {
                open(providerName, now);
                return;
            }

            consecutiveFailures++;
            if (consecutiveFailures >= config.getFailureThreshold()) {
                open(providerName, now);
            }
        }

        private void open(String providerName, Instant now) {
            if (status != CircuitStatus.OPEN) {
                log.warn("AI provider circuit opened: {}", providerName);
            }
            status = CircuitStatus.OPEN;
            openedAt = now;
        }
    }

    private enum CircuitStatus {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
