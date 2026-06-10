package com.mentify.exception;

public class KeycloakRoleAssignmentException extends RuntimeException {

    public KeycloakRoleAssignmentException(String message) {
        super(message);
    }

    public KeycloakRoleAssignmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
