package com.mentify.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class AiQuotaExceededException extends AiProviderException {
    public AiQuotaExceededException(String message) {
        super(message);
    }
}
