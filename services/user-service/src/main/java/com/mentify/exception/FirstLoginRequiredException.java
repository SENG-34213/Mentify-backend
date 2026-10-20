package com.mentify.exception;

public class FirstLoginRequiredException extends RuntimeException {

    public FirstLoginRequiredException() {
        super("Complete account setup before signing in");
    }
}
