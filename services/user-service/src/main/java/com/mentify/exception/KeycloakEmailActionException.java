package com.mentify.exception;

public class KeycloakEmailActionException extends RuntimeException {

    public KeycloakEmailActionException(String message) {
        super(message);
    }

    public KeycloakEmailActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
