package com.mentify.material.exception;

public class MaterialValidationException extends RuntimeException {

    public MaterialValidationException(String message) {
        super(message);
    }

    public MaterialValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
