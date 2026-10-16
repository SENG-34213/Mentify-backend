package com.mentify.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class AiQuotaExceededException extends AiProviderException {
    private final long retryAfterSeconds;

    public AiQuotaExceededException(String message) {
        super(message);
        this.retryAfterSeconds = 0;
    }

    public AiQuotaExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(0, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
