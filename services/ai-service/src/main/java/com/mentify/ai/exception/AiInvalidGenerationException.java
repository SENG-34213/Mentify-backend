package com.mentify.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class AiInvalidGenerationException extends AiProviderException {

    public AiInvalidGenerationException(String message) {
        super(message);
    }

    public AiInvalidGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
