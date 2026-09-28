package com.mentify.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AiInvalidDocumentException extends AiProviderException {

    public AiInvalidDocumentException(String message) {
        super(message);
    }

    public AiInvalidDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
