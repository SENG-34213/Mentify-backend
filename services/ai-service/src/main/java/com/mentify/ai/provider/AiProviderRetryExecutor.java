package com.mentify.ai.provider;

import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.exception.AiProviderException;
import com.mentify.ai.exception.AiProviderRateLimitException;
import com.mentify.ai.exception.AiProviderUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiProviderRetryExecutor {
    private final AiProviderProperties properties;

    public <T> T execute(String providerName, Supplier<T> operation) {
        AiProviderProperties.RetryConfig retryConfig = properties.getRetry();
        int maxAttempts = Math.max(1, retryConfig.getMaxAttempts());
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (RuntimeException ex) {
                lastException = ex;
                if (!retryConfig.isEnabled() || !isRetryable(ex) || attempt >= maxAttempts) {
                    throw ex;
                }

                Duration backoff = retryConfig.getBackoff();
                log.warn("Retrying AI provider request provider={} attempt={}/{} reason={}",
                        providerName, attempt + 1, maxAttempts, ex.getClass().getSimpleName());
                sleep(backoff);
            }
        }

        throw lastException == null
                ? new AiProviderException("AI provider request failed")
                : lastException;
    }

    private boolean isRetryable(RuntimeException ex) {
        return ex instanceof AiProviderRateLimitException
                || ex instanceof AiProviderUnavailableException;
    }

    private void sleep(Duration backoff) {
        if (backoff == null || backoff.isZero() || backoff.isNegative()) {
            return;
        }

        try {
            Thread.sleep(backoff.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Interrupted while retrying AI provider request", ex);
        }
    }
}
