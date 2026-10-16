package com.mentify.service.registration;

public class RegistrationEmailDeliveryException extends RuntimeException {

    public RegistrationEmailDeliveryException() {
        super("Account setup email could not be delivered");
    }
}
