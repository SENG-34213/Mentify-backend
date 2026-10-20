package com.mentify.service.registration;

public class RegistrationEmailDeliveryException extends RuntimeException {

    public RegistrationEmailDeliveryException() {
        super("Registration credentials email could not be delivered");
    }
}
