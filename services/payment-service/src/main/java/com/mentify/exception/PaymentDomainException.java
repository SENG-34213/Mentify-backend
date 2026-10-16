package com.mentify.exception;

import org.springframework.http.HttpStatus;

public class PaymentDomainException extends RuntimeException {

    private final HttpStatus status;

    public PaymentDomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
