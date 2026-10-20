package com.mentify.service.registration;

public class TemporaryCredentialsEmailException extends RuntimeException {

    public TemporaryCredentialsEmailException() {
        super("Temporary credentials email could not be delivered");
    }
}
