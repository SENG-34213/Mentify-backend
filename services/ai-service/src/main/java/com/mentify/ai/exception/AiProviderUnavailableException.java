package com.mentify.ai.exception;
 
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
 
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AiProviderUnavailableException extends AiProviderException {
    public AiProviderUnavailableException(String message) {
        super(message);
    }
    public AiProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
