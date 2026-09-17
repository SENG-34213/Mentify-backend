package com.mentify.ai.exception;
 
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
 
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class AiProviderConfigurationException extends AiProviderException {
    public AiProviderConfigurationException(String message) {
        super(message);
    }
}
