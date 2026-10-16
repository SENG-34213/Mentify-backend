package com.mentify.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class AiProviderRateLimitException extends AiProviderException {
    public AiProviderRateLimitException(String message) {
        super(message);
    }

    public AiProviderRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
