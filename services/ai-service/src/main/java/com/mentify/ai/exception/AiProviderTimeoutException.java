package com.mentify.ai.exception;
 
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
 
@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
public class AiProviderTimeoutException extends AiProviderException {
    public AiProviderTimeoutException(String message) {
        super(message);
    }
    public AiProviderTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
