package com.mentify.ai.exception;
 
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
 
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class AiEmptyResponseException extends AiProviderException {
    public AiEmptyResponseException(String message) {
        super(message);
    }
}
